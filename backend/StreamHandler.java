package backend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

/**
 * Class that is effectively the outward facing API of the backend for the front end to hit
 * for all desired operations.
 * @author patrickmarx
 *
 */
public class StreamHandler {

	private Map<Integer, DataPipeline> dataPipelines;
	
	public StreamHandler() {
		this.dataPipelines = new HashMap<Integer, DataPipeline>();
	}
	
	/**
	 * Access point for the front end to get the most recent emage from
	 * a specific datasource
	 * @param id The ID of the datapipeline of the datasource to pull the most recent emage from
	 * @return
	 * @throws Exception 
	 */
	public Emage getLatestEmageByPipelineID(int id) throws Exception {
		//TODO: Probably need some sort of access control here or earlier
		DataPipeline dp = this.dataPipelines.get(id);
		if (dp != null) {
			return dp.emageStream.getMostRecentEmage();
		} else {
			throw new Exception();
		}
		
	}
		
	/**
	 * Access point for the front end to get the most recent set of points from
	 * a specific datasource
	 * @param id The ID of the datapipeline of the datasource to pull the most recent points from
	 * @return
	 * @throws Exception 
	 */
	public ArrayList<STTPoint> getLatestPointsByPipelineID(int id) throws Exception {
		//TODO: probably need some sort of access control here or earlier
		DataPipeline dp = this.dataPipelines.get(id);
		if (dp != null) {
			return dp.pointStream.getMostRecentPoints();
		} else {
			throw new Exception();
		}
	}
	
	/**
	 * Access point for updating the polling frequency of a given pointstream
	 * @param id identifier of the DataPipeline holding the desired pointstream (datasource)
	 * @param newPollTimeMS value in Milleseconds the polling time will be set to
	 * @throws Exception 
	 */
	public void setPointPollTimeByPipelineID(int id, int newPollTimeMS) throws Exception {
		DataPipeline dp = this.dataPipelines.get(id);
		if (dp != null) {
			dp.pointStream.setPollingTimeMS(newPollTimeMS);
		} else {
			throw new Exception();
		}
	}
	
	/**
	 * Access point for updating the creation frequency of a given emagestream's emages
	 * @param id identifier of the DataPipeline holding the desired emagestream (datasource)
	 * @param newRateMS value in Milleseconds the creation emage rate will be set to
	 * @throws Exception 
	 */
	public void setEmageCreationRateByPipelineID(int id, int newRateMS) throws Exception {
		DataPipeline dp = this.dataPipelines.get(id);
		if (dp != null) {
			dp.pointStream.setPollingTimeMS(newRateMS);
		} else {
			throw new Exception();
		}
	}
	
	/**
	 * Constructs a new data pipeline and adds it to our storage map indexed by it's ID, and then
	 * Starts the PointStream and EmageStream instances on their own threads to start processing Data
	 * @param json The json object from the front end request used to create a new pipeline
	 * @return the int ID of the newly created pipeline
	 */
	protected int buildAndStartNewPipeline(GsonNewPipelineRequest request) {		
		LatLong boundingBoxNW = new LatLong(request.NWlat, request.NWlong);
		LatLong boundingBoxSE = new LatLong(request.SElat, request.SElong);
		
		GeoParams geoParams = new GeoParams(request.resolutionX, request.resolutionY, boundingBoxNW, boundingBoxSE);
		AuthFields authFields = new AuthFields("", "", "", "");
		WrapperParams wrapperParams = new WrapperParams(request.source, request.theme);
		
		WrapperFactory.WrapperType type = WrapperFactory.WrapperType.valueOf(request.wrapperType);
		AbstractDataWrapper tw = WrapperFactory.getWrapperInstance(type, wrapperParams, authFields, geoParams);
		
		PointStream ps = new PointStream(tw, request.pointPollingTimeMS);
		
		EmageBuilder eb = new EmageBuilder(ps, EmageBuilder.Operator.valueOf(request.operatorType));
		EmageStream es = new EmageStream(eb, request.emageCreationRateMS, request.emageWindowLength);
		
		//Add the pipeline to our collection by it's index so we can reaccess it
		final DataPipeline p = new DataPipeline(ps, es);
		this.dataPipelines.put(p.pipelineID, p);
		
		//Start the streams
		Thread pointThread = new Thread(new Runnable(){
			@Override
			public void run() {
				while (true) {
					System.out.println(p.pointStream.getNextPoint());
				}
			} 	
		});	
		Thread emageThread = new Thread(new Runnable(){
			@Override
			public void run() {
				while (true) {
					System.out.println(p.emageStream.getNextEmage());
				}
			}
		});
		pointThread.start();
		emageThread.start();
		
		return p.pipelineID;
	}
}
