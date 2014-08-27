package org.cytoscape.rest.internal.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;



/**
 * 
 * Algorithmic resources.  Essentially, this is a high-level task executor.
 * 
 */
@Singleton
@Path("/v1/apply")
public class AlgorithmicResource extends AbstractResource {

	@Context
	private TaskMonitor headlessTaskMonitor;

	@Context
	private CyLayoutAlgorithmManager layoutManager;

	
	/**
	 * 
	 * @summary Apply layout to a network
	 * 
	 * @param algorithmName Name of layout algorithm ("circular", "force-directed", etc.)
	 * @param networkId Target network SUID
	 * 
	 * @return Success message
	 */
	@GET
	@Path("/layouts/{algorithmName}/{networkId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response applyLayout(@PathParam("algorithmName") String algorithmName, @PathParam("networkId") Long networkId) {
		final CyNetwork network = getCyNetwork(networkId);
		final Collection<CyNetworkView> views = this.networkViewManager.getNetworkViews(network);
		if (views.isEmpty()) {
			throw getError("Could not find view for the network with SUID: " + networkId, Response.Status.NOT_FOUND);
		}

		final CyNetworkView view = views.iterator().next();
		final CyLayoutAlgorithm layout = this.layoutManager.getLayout(algorithmName);
		if(layout == null) {
			throw getError("No such layout algorithm: " + algorithmName, Response.Status.NOT_FOUND);
		}
		
		final TaskIterator itr = layout.createTaskIterator(view, layout.getDefaultLayoutContext(),
				CyLayoutAlgorithm.ALL_NODE_VIEWS, "");
		try {
			itr.next().run(headlessTaskMonitor);
		} catch (Exception e) {
			throw getError(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
		return Response.status(Response.Status.OK).entity("{\"message\":\"Layout finished.\"}").build();
	}



	
	/**
	 * Apply Visual Style to a network.
	 * 
	 * @param styleName Visual Style name (title)
	 * @param networkId Target network SUID
	 * 
	 * @return Success message.
	 */
	@GET
	@Path("/styles/{styleName}/{networkId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response applyStyle(@PathParam("styleName") String styleName, @PathParam("networkId") Long networkId) {

		final CyNetwork network = getCyNetwork(networkId);
		final Set<VisualStyle> styles = vmm.getAllVisualStyles();
		VisualStyle targetStyle = null;
		for (final VisualStyle style : styles) {
			final String name = style.getTitle();
			if (name.equals(styleName)) {
				targetStyle = style;
				break;
			}
		}

		if (targetStyle == null) {
			throw getError("Visual Style does not exist: " + styleName, Response.Status.NOT_FOUND);
		}

		Collection<CyNetworkView> views = this.networkViewManager.getNetworkViews(network);
		if (views.isEmpty()) {
			throw getError("Network view does not exist for the network with SUID: " + networkId, Response.Status.NOT_FOUND);
		}

		final CyNetworkView view = views.iterator().next();
		vmm.setVisualStyle(targetStyle, view);
		vmm.setCurrentVisualStyle(targetStyle);
		targetStyle.apply(view);

		return Response.status(Response.Status.OK).entity("{\"message\":\"Visual Style applied.\"}").build();
	}

	/**
	 * @summary Get list of available layout algorithm names
	 * 
	 * @return List of layout algorithm names.
	 */
	@GET
	@Path("/layouts")
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<String> getLayoutNames() {
		final Collection<CyLayoutAlgorithm> layouts = layoutManager.getAllLayouts();
		final List<String> layoutNames = new ArrayList<String>();
		for (final CyLayoutAlgorithm layout : layouts) {
			layoutNames.add(layout.getName());
		}
		return layoutNames;
	}
	

	/**
	 * @summary Get list of all Visual Style names.
	 * 
	 * @return List of Style names.
	 * 
	 */
	@GET
	@Path("/styles")
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<String> getStyleNames() {
		final Set<VisualStyle> styles = vmm.getAllVisualStyles();
		final List<String> styleNames = new ArrayList<String>();
		for (final VisualStyle style : styles) {
			styleNames.add(style.getTitle());
		}
		return styleNames;
	}
}