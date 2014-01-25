package kn.uni.gis.softtasks;

import java.io.StringWriter;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import kn.uni.gis.dataimport.util.CPoint;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;


public class TestClass {
	private DatatypeFactory fact;
	// private static final String TEST_BASE_URI =
	// "http://91.228.52.101:9090/game";

	private static final String TEST_BASE_URI = "http://localhost:9090/game";

	@Before
	public void before() throws DatatypeConfigurationException {
		fact = DatatypeFactory.newInstance();
	}

	@Test
	public void test() throws JAXBException {

		Location location = new Location();
		location.setGamerName("Wilhelm");

		location.setLon(13.18f);
		location.setLat(52.40f);

		location.setTimestamp(fact
				.newXMLGregorianCalendar(new GregorianCalendar()));

		JAXBContext newInstance = JAXBContext
				.newInstance("kn.uni.gis.softtasks");

		newInstance.createMarshaller().marshal(location, System.out);

		// <location gamerName="Hans" lon="17.0" lat="18.0"
		// timestamp="2013-06-22 12:42:51.777 MESZ" xmlns="http://soft.gis/v1"/>

	}

	@Test
	@Ignore
	public void testClient() throws JAXBException, InterruptedException {

		Client create = Client.create();

		WebResource resource = create.resource(TEST_BASE_URI);

		String post = resource.put(String.class);

		System.out.println("1");

		WebResource foxRes = create.resource(getFoxUri(post));
		WebResource hunt1Res = create.resource(getHunterUri(post, "hunter1"));
		WebResource hunt2Res = create.resource(getHunterUri(post, "hunter2"));

		long fox = System.currentTimeMillis() - 500000;
		long other1 = fox + 1000;
		long other2 = fox + 3000;

		Random rand = new Random();

		for (CPoint p : foxCoords(0, 0)) {
			fox = sendLocation("Maggie", foxRes, fox, rand, p);
		}
		for (CPoint p : hunt1Coords(0, 0)) {
			other1 = sendLocation("Homer", hunt1Res, other1, rand, p);
		}
		for (CPoint p : hunt2Coords(0, 0)) {
			other2 = sendLocation("Marge", hunt2Res, other2, rand, p);
		}

		// HUNTER1
		// new CPoint(lat_offset+47.676831188125945, 9.167060852050781);
		// new CPoint(lat_offset+47.67347921444501, 9.171867370605469);
		// new CPoint(lat_offset+47.67082059945156, 9.173240661621094);
		// new CPoint(lat_offset+47.667468239668764, 9.173412322998047);
		// new CPoint(lat_offset+47.66504053437091, 9.172039031982422);
		// new CPoint(lat_offset+47.663075166428364, 9.170665740966797);
		// new CPoint(lat_offset+47.66110972448931, 9.172897338867188);
		// new CPoint(lat_offset+47.660647256808595, 9.17581558227539);

		// new CPoint(lat_offset+47.64110425422311, 9.173412322998047);
		// new CPoint(lat_offset+47.644111346012295, 9.173240661621094);
		// new CPoint(lat_offset+47.64758085221801, 9.176673889160156);
		// new CPoint(lat_offset+47.65035629125809, 9.173583984375);
		// new CPoint(lat_offset+47.65290031414818, 9.170150756835938);
		// new CPoint(lat_offset+47.65555984193563, 9.166717529296875);
		// new CPoint(lat_offset+47.65717861861977, 9.167232513427734);
		// new CPoint(lat_offset+47.6572942436054, 9.169120788574219);
		// new CPoint(lat_offset+47.65683174212656, 9.170150756835938);
		// new CPoint(lat_offset+47.656137982226795, 9.171867370605469);
		// new CPoint(lat_offset+47.65925982918611, 9.174613952636719);
	}

	private long sendLocation(String gamerName, WebResource foxRes, long fox,
			Random rand, CPoint p) throws InterruptedException {
		Location location = new Location();
		location.setLat((float) p.getLat());
		location.setLon((float) p.getLon());

		GregorianCalendar c = new GregorianCalendar();
		c.setTimeInMillis(fox);
		fox += (3000 + rand.nextInt(5000));
		location.setTimestamp(fact.newXMLGregorianCalendar(c));

		location.setGamerName(gamerName);
		foxRes.post(location);
		return fox;
	}

	private String getHunterUri(String post, String string) {
		return TEST_BASE_URI + "/" + post + "/" + string;
	}

	private String getFoxUri(String post) {
		return TEST_BASE_URI + "/" + post;
	}

	public static List<CPoint> foxCoords(double lon_offset, double let_offset) {
		return ImmutableList.of(new CPoint(lon_offset + 47.66400005467765,
				9.173583984375 + let_offset, false), new CPoint(
				lon_offset + 47.662150261792256,
				9.172210693359375 + let_offset, false), new CPoint(lon_offset
				+ lon_offset + 47.66018478503117,
				9.170494079589844 + let_offset, false), new CPoint(lon_offset
				+ lon_offset + 47.658219234272856,
				9.172210693359375 + let_offset, false), new CPoint(lon_offset
				+ lon_offset + 47.65810361133558,
				9.17581558227539 + let_offset, false), new CPoint(lon_offset
				+ lon_offset + 47.660647256808595,
				9.178047180175781 + let_offset, false));
	}

	public static List<CPoint> hunt1Coords(double lon_offset, double let_offset) {
		return ImmutableList.of(new CPoint(lon_offset + 47.676831188125945,
				9.167060852050781 + let_offset, false), new CPoint(
				lon_offset + 47.67347921444501, 9.171867370605469 + let_offset,
				false), new CPoint(lon_offset + 47.67082059945156,
				9.173240661621094 + let_offset, false), new CPoint(
				lon_offset + 47.667468239668764,
				9.173412322998047 + let_offset, false), new CPoint(
				lon_offset + 47.66504053437091, 9.172039031982422 + let_offset,
				false), new CPoint(lon_offset + 47.663075166428364,
				9.170665740966797 + let_offset, false), new CPoint(
				lon_offset + 47.66110972448931, 9.172897338867188 + let_offset,
				false), new CPoint(lon_offset + 47.660647256808595,
				9.17581558227539 + let_offset, false));
	}

	public static List<CPoint> hunt2Coords(double lon_offset, double let_offset) {
		return ImmutableList.of(new CPoint(lon_offset + 47.64110425422,
				9.173412322998047 + let_offset, false), new CPoint(
				lon_offset + 47.644111346012295,
				9.173240661621094 + let_offset, false), new CPoint(
				lon_offset + 47.64758085221801, 9.176673889160156 + let_offset,
				false), new CPoint(lon_offset + 47.65035629125809,
				9.173583984375 + let_offset, false), new CPoint(
				lon_offset + 47.65290031414818, 9.170150756835938 + let_offset,
				false), new CPoint(lon_offset + 47.65555984193563,
				9.166717529296875 + let_offset, false), new CPoint(
				lon_offset + 47.65717861861977, 9.167232513427734 + let_offset,
				false), new CPoint(lon_offset + 47.6572942436054,
				9.169120788574219 + let_offset, false), new CPoint(
				lon_offset + 47.65683174212656, 9.170150756835938 + let_offset,
				false), new CPoint(lon_offset + 47.656137982226795,
				9.171867370605469 + let_offset, false), new CPoint(
				lon_offset + 47.65925982918611, 9.174613952636719 + let_offset,
				false));
	}
}
