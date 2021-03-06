package org.cytoscape.rest.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


import java.awt.Color;
import java.awt.Paint;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.rest.internal.resource.NetworkViewResource;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NetworkViewResourceTest extends BasicResourceTest {

	private ObjectMapper mapper = new ObjectMapper();

	
	@Override
	protected Application configure() {
		return new ResourceConfig(NetworkViewResource.class);
	}
	
	
	@Test
	public void testConstructor() throws Exception {
		NetworkViewResource nvr = new NetworkViewResource();
		assertNotNull(nvr);
	}

	@Test
	public void testCreateNetworkView() throws Exception {
		final Long suid = network.getSUID();
		
		final Entity<String> entity = Entity.entity("", MediaType.APPLICATION_JSON_TYPE);
		Response result = target("/v1/networks/" + suid.toString() + "/views").request().post(entity);
		assertNotNull(result);
		System.out.println("res: " + result.toString());
		assertFalse(result.getStatus() == 500);
		assertEquals(201, result.getStatus());
		
		final String body = result.readEntity(String.class);
		System.out.println("BODY: " + body);
		final JsonNode root = mapper.readTree(body);
		assertTrue(root.isObject());
		assertNotNull(root.get("networkViewSUID").asLong());
		
	}
	
	@Test
	public void testGetNetworkViews() throws Exception {
		
		final Long suid = network.getSUID();
		final Long viewSuid = view.getSUID();
		
		Response result = target("/v1/networks/" + suid.toString() + "/views").request().get();
		assertNotNull(result);
		String body = result.readEntity(String.class);
		System.out.println("Result = " + result);

		final JsonNode root = mapper.readTree(body);
		assertNotNull(root);
		System.out.println(root);
		assertTrue(root.isArray());
		assertEquals(1, root.size());
		assertEquals(viewSuid, Long.valueOf(root.get(0).asLong()));
	}

	@Test
	public void testGetNetworkVisualProp() throws Exception {
		
		final Long suid = network.getSUID();
		final Long viewSuid = view.getSUID();
		
		Response result = target("/v1/networks/" + suid.toString() + "/views/" + viewSuid + "/network/NETWORK_SIZE").request().get();
		assertNotNull(result);
		String body = result.readEntity(String.class);
		System.out.println("Result = " + result);

		JsonNode root = mapper.readTree(body);
		assertNotNull(root);
		System.out.println(root);
		assertTrue(root.isObject());
		assertEquals((Double)550.0, (Double)root.get("value").asDouble());
	}

	@Test
	public void testGetViewsOf() throws Exception {
		
		final Long suid = network.getSUID();
		final Long viewSuid = view.getSUID();
		
		Response result = target("/v1/networks/" + suid.toString() + "/views/" + viewSuid + "/nodes").request().get();
		assertNotNull(result);
		String body = result.readEntity(String.class);
		System.out.println("Result = " + result);

		JsonNode root = mapper.readTree(body);
		assertNotNull(root);
		System.out.println(root);
		assertTrue(root.isArray());
		assertEquals(view.getNodeViews().size(), root.size());
		
		result = target("/v1/networks/" + suid.toString() + "/views/" + viewSuid + "/edges").request().get();
		assertNotNull(result);
		body = result.readEntity(String.class);
		System.out.println("Result = " + result);

		root = mapper.readTree(body);
		assertNotNull(root);
		System.out.println(root);
		assertTrue(root.isArray());
		assertEquals(view.getEdgeViews().size(), root.size());
		
		
		result = target("/v1/networks/" + suid.toString() + "/views/" + viewSuid + "/network").request().get();
		assertNotNull(result);
		body = result.readEntity(String.class);
		System.out.println("Network Result = " + result);

		root = mapper.readTree(body);
		assertNotNull(root);
		System.out.println(root);
		assertTrue(root.isArray());
		assertEquals(12, root.size());
	}


	@Test
	public void testGetNetworkView() throws Exception {
		
		final Long suid = network.getSUID();
		final Long viewSuid = view.getSUID();
		
		// Test invalid View ID
		String result = target("/v1/networks/" + suid.toString() + "/views/1234567").request().get(
				String.class);
		assertNotNull(result);
		assertEquals("{}", result);
		
		// Test valid
		result = target("/v1/networks/" + suid.toString() + "/views/" + viewSuid.toString()).request().get(
				String.class);
		assertNotNull(result);
		System.out.println("Result = " + result);
		// TODO: Verify JSON writer calls
		
	}
	

	@Test
	public void testGetNetworkViewWrite() throws Exception {
		
		final Long suid = network.getSUID();
		final Long viewSuid = view.getSUID();
		
		// Test valid
		String result = target("/v1/networks/" + suid.toString() + "/views/" + viewSuid.toString()).queryParam("file", "networkViewWrite.tmp").request().get(
				String.class);
		assertNotNull(result);
		System.out.println("Result = " + result);
		// TODO: Verify JSON writer calls
		
	}

	@Test
	public void testGetFirstNetworkImage() throws Exception {
		
		verify(graphicsWriterManager, never()).getFactory("png");
		verify(graphicsWriterManager, never()).getFactory("svg");
		verify(graphicsWriterManager, never()).getFactory("pdf");
		
		final Long suid = network.getSUID();
		
		Response result = target("/v1/networks/" + suid.toString() + "/views/first.png").request().get();
		assertNotNull(result);
		assertEquals(200, result.getStatus());
		verify(graphicsWriterManager).getFactory("png");
		System.out.println("Result = " + result);
		result = target("/v1/networks/" + suid.toString() + "/views/first.svg").request().get();
		assertNotNull(result);
		assertEquals(200, result.getStatus());
		verify(graphicsWriterManager).getFactory("svg");
		System.out.println("Result = " + result);
		result = target("/v1/networks/" + suid.toString() + "/views/first.pdf").request().get();
		assertNotNull(result);
		assertEquals(200, result.getStatus());
		verify(graphicsWriterManager).getFactory("pdf");
		
		verify(presentationWriterFactory, times(3)).createWriter(any(ByteArrayOutputStream.class), eq(renderingEngine));
		
		// TODO: Check that proper proportions were set.
	}
	
	@Test
	public void testGetNetworkImage() throws Exception {
		
		verify(graphicsWriterManager, never()).getFactory("png");
		verify(graphicsWriterManager, never()).getFactory("svg");
		verify(graphicsWriterManager, never()).getFactory("pdf");
		
		final Long suid = network.getSUID();
		final Long viewSuid = view.getSUID();
		
		Response result = target("/v1/networks/" + suid.toString() + "/views/"+ viewSuid.toString() +".png").request().get();
		assertNotNull(result);
		assertEquals(200, result.getStatus());
		verify(graphicsWriterManager).getFactory("png");
		System.out.println("Result = " + result);
		result = target("/v1/networks/" + suid.toString() + "/views/"+ viewSuid.toString() +".svg").request().get();
		assertNotNull(result);
		assertEquals(200, result.getStatus());
		verify(graphicsWriterManager).getFactory("svg");
		System.out.println("Result = " + result);
		result = target("/v1/networks/" + suid.toString() + "/views/"+ viewSuid.toString() +".pdf").request().get();
		assertNotNull(result);
		assertEquals(200, result.getStatus());
		verify(graphicsWriterManager).getFactory("pdf");
		System.out.println("Result = " + result);
		
		verify(presentationWriterFactory, times(3)).createWriter(any(ByteArrayOutputStream.class), eq(renderingEngine));
		
		// TODO: Check that proper proportions were set.
	}


	@Test
	public void testGetFirstView() throws Exception {
		
		final Long suid = network.getSUID();
		Response result = target("/v1/networks/" + suid.toString() + "/views/first").request().get();
		assertNotNull(result);
		assertEquals(200, result.getStatus());
	}
	
	@Test
	public void testGetViewCount() throws Exception {
		
		final Long suid = network.getSUID();
		
		String result = target("/v1/networks/" + suid.toString() + "/views/count").request().get(
				String.class);
		assertNotNull(result);
		final JsonNode root = mapper.readTree(result);
		assertNotNull(root);
		System.out.println(root);
		assertEquals(1, root.get("count").asInt());
	}
	
	@Test
	public void testGetView() throws Exception {
		
		final Long suid = network.getSUID();
		final Long viewSuid = view.getSUID();
		final CyNode node = network.getNodeList().iterator().next();
		
		Response result = target("/v1/networks/" + suid.toString() + "/views/" + viewSuid + "/nodes/" + node.getSUID()).request().get();
		assertNotNull(result);
		System.out.println("res: " + result.toString());
		assertFalse(result.getStatus() == 500);
		assertEquals(200, result.getStatus());
		
		final JsonNode root = mapper.readTree(result.readEntity(String.class));
		assertNotNull(root);
		System.out.println(root);
		assertTrue(root.isArray());
		assertEquals(lexicon.getAllDescendants(BasicVisualLexicon.NODE).size(), root.size());
		
		final Map<String, String> vps = new HashMap<>();
		for(JsonNode n: root) {
			vps.put(n.get("visualProperty").asText(), n.get("value").asText());
		}
		
		assertTrue(vps.keySet().contains("NODE_PAINT"));
		assertTrue(vps.keySet().contains("NODE_BORDER_PAINT"));
		assertEquals("#000000", vps.get("NODE_BORDER_PAINT"));
	}


	@Test
	public void testGetNetworkViewVP() throws Exception {
		final Long suid = network.getSUID();
		final Long viewSuid = view.getSUID();
		
		final String vp = "NETWORK_BACKGROUND_PAINT";
		
		Response result = target("/v1/networks/" + suid.toString() + "/views/" + viewSuid + "/network/" + vp).request().get();
		assertNotNull(result);
		System.out.println("res: " + result.toString());
		assertFalse(result.getStatus() == 500);
		assertEquals(200, result.getStatus());
		
		final JsonNode root = mapper.readTree(result.readEntity(String.class));
		assertNotNull(root);
		System.out.println(root);
		assertTrue(root.isObject());
		
		final String vpName = root.get("visualProperty").asText();
		assertEquals(vp, vpName);
		
		final String val = root.get("value").asText();
		assertEquals("#ffffff".toUpperCase(), val.toUpperCase());
	}


	@Test
	public void testUpdateNetworkViewVP() throws Exception {
		final Long suid = network.getSUID();
		final Long viewSuid = view.getSUID();
		
		final String vp = "NETWORK_BACKGROUND_PAINT";
		final String newVal = "[{"
				+ "\"visualProperty\": \"" + vp + "\","
				+ "\"value\": \"red\" }]";
		
	
		
		Entity<String> entity = Entity.entity(newVal, MediaType.APPLICATION_JSON_TYPE);
		Response result = target("/v1/networks/" + suid.toString() + "/views/" + viewSuid + "/network").request().put(entity);
		assertNotNull(result);
		System.out.println("res: " + result.toString());
		assertFalse(result.getStatus() == 500);
		assertEquals(200, result.getStatus());
		
		final Paint newValue = view.getVisualProperty(BasicVisualLexicon.NETWORK_BACKGROUND_PAINT);
		assertEquals(Color.RED, newValue);
	}


	@Test
	public void testUpdateView() throws Exception {
		final Long suid = network.getSUID();
		final Long viewSuid = view.getSUID();
	
		// Test node VPs
		final CyNode node = network.getNodeList().iterator().next();
		String vp = BasicVisualLexicon.NODE_FILL_COLOR.getIdString();
		String newVal = "[{"
				+ "\"visualProperty\": \"" + vp + "\","
				+ "\"value\": \"blue\" }]";
		testViewUpdates(newVal, suid, viewSuid, "nodes", node.getSUID());
		Paint newColor = view.getNodeView(node).getVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR);
		assertEquals(Color.BLUE, newColor);
		
		vp = BasicVisualLexicon.NODE_WIDTH.getIdString();
		newVal = "[{"
				+ "\"visualProperty\": \"" + vp + "\","
				+ "\"value\": 200.0 }]";
		testViewUpdates(newVal, suid, viewSuid, "nodes", node.getSUID());
		Double newWidth = view.getNodeView(node).getVisualProperty(BasicVisualLexicon.NODE_WIDTH);
		assertEquals(new Double(200), newWidth);
		
		// Test edges
		final CyEdge edge = network.getEdgeList().iterator().next();
		vp = BasicVisualLexicon.EDGE_WIDTH.getIdString();
		newVal = "[{"
				+ "\"visualProperty\": \"" + vp + "\","
				+ "\"value\": 22.1 }]";
		testViewUpdates(newVal, suid, viewSuid, "edges", edge.getSUID());
		newWidth = view.getEdgeView(edge).getVisualProperty(BasicVisualLexicon.EDGE_WIDTH);
		assertEquals(new Double(22.1), newWidth);
	}
	
	private void testViewUpdates(final String newVal, final Long suid, final Long viewSuid, final String type, final Long objId) {
		Entity<String> entity = Entity.entity(newVal, MediaType.APPLICATION_JSON_TYPE);
		Response result = target("/v1/networks/" + suid.toString() + "/views/" + viewSuid + "/" + type + "/" + objId).request().put(entity);
		assertNotNull(result);
		System.out.println("res: " + result.toString());
		assertFalse(result.getStatus() == 500);
		assertEquals(200, result.getStatus());
	}
	
	
	
	private final String createViewJson(Map<String, Long> idmap) throws Exception {
		final JsonFactory factory = new JsonFactory();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		JsonGenerator generator = null;
		
		generator = factory.createGenerator(stream);
		generator.writeStartArray();
		
		// Node 1 Updates:
		generator.writeStartObject();
		generator.writeNumberField("SUID", idmap.get("n1"));

		generator.writeArrayFieldStart("view");
		
		generator.writeStartObject();
		generator.writeStringField("visualProperty", "NODE_FILL_COLOR");
		generator.writeStringField("value", "yellow");
		generator.writeEndObject();
		
		generator.writeStartObject();
		generator.writeStringField("visualProperty", "NODE_SIZE");
		generator.writeNumberField("value", 130);
		generator.writeEndObject();
		
		generator.writeEndArray();
		
		generator.writeEndObject();
		
		generator.writeEndArray();
		generator.close();
		final String result = stream.toString("UTF-8");
		stream.close();
		return result;
	}
	
	private final String createEdgeViewJson(final Collection<CyEdge> edges) throws Exception {
		final JsonFactory factory = new JsonFactory();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		JsonGenerator generator = null;
		
		generator = factory.createGenerator(stream);
		generator.writeStartArray();
		
		for(CyEdge edge: edges) {
			generator.writeStartObject();
			
			generator.writeNumberField("SUID", edge.getSUID());
			
			generator.writeArrayFieldStart("view");
			generator.writeStartObject();
			generator.writeStringField("visualProperty", "EDGE_WIDTH");
			generator.writeNumberField("value", 22);
			generator.writeEndObject();

			generator.writeStartObject();
			generator.writeStringField("visualProperty", "EDGE_STROKE_UNSELECTED_PAINT");
			generator.writeStringField("value", "#00FF00");
			generator.writeEndObject();
			generator.writeEndArray();

			generator.writeEndObject();
		}

		generator.writeEndArray();
		generator.close();
		
		final String result = stream.toString("UTF-8");
		stream.close();
		return result;
	}
	
	@Test
	public void testUpdateViews() throws Exception {
		final List<CyNode> nodes = network.getNodeList();
		
		Map<String, Long> idmap = nodes.stream()
			.collect(Collectors.toMap(
						node->network.getRow(node).get(CyNetwork.NAME, String.class), 
						n->n.getSUID()
					)
				);
		
		System.out.println("Nodes: " + idmap);
		final Long suid = network.getSUID();
		final Long viewSuid = view.getSUID();
		final String newVal = createViewJson(idmap);
		Entity<String> entity = Entity.entity(newVal, MediaType.APPLICATION_JSON_TYPE);
		Response result = target("/v1/networks/" + suid.toString() + "/views/" + viewSuid + "/nodes").request().put(entity);
		assertNotNull(result);
		System.out.println("res: " + result.toString());
		assertFalse(result.getStatus() == 500);
		assertEquals(200, result.getStatus());
		final View<CyNode> nv1 = view.getNodeView(network.getNode(idmap.get("n1")));
		
		assertEquals((Double)130.0, nv1.getVisualProperty(BasicVisualLexicon.NODE_SIZE));
		assertEquals(Color.YELLOW, nv1.getVisualProperty(BasicVisualLexicon.NODE_FILL_COLOR));
		
		// Test bulk-update edges
		final String newEdgeVal = createEdgeViewJson(network.getEdgeList());
		entity = Entity.entity(newEdgeVal, MediaType.APPLICATION_JSON_TYPE);
		result = target("/v1/networks/" + suid.toString() + "/views/" + viewSuid + "/edges").request().put(entity);
		assertNotNull(result);
		System.out.println("res: " + result.toString());
		assertFalse(result.getStatus() == 500);
		assertEquals(200, result.getStatus());
		
		view.getEdgeViews().stream()
			.forEach(ev->testUpdateEdgeViewsResult(ev));
	}
	
	private final void testUpdateEdgeViewsResult(final View<CyEdge> ev) {
		assertEquals((Double)22.0, ev.getVisualProperty(BasicVisualLexicon.EDGE_WIDTH));
		assertEquals(Color.green, ev.getVisualProperty(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT));
	}
	
	@Test
	public void testDeleteAllNetworkViews() throws Exception {
		
		final Response result = target("/v1/networks/" + network.getSUID().toString() + "/views").request().delete();
		assertNotNull(result);
		assertEquals(200, result.getStatus());
	}
	
	@Test
	public void testDeleteFirstView() throws Exception {
		final Long suid = network.getSUID();
		
		final Response result = target("/v1/networks/" + suid.toString() + "/views/first").request().delete();
		assertNotNull(result);
		assertEquals(200, result.getStatus());
	}
	
}