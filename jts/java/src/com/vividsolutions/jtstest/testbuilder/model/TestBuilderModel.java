package com.vividsolutions.jtstest.testbuilder.model;

import java.io.*;
import java.util.*;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.io.*;
import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jtstest.test.TestCaseList;
import com.vividsolutions.jtstest.test.Testable;
import com.vividsolutions.jtstest.testbuilder.ui.style.BasicStyle;
import com.vividsolutions.jtstest.testrunner.TestReader;
import com.vividsolutions.jtstest.testrunner.TestRun;
import com.vividsolutions.jtstest.util.*;
import com.vividsolutions.jtstest.testbuilder.ui.*;
import com.vividsolutions.jtstest.testbuilder.io.shapefile.Shapefile;

public class TestBuilderModel 
{
  public static int MAX_DISPLAY_POINTS = 2000;

  protected static boolean showingGrid = true;
  protected static boolean showingOrientations = false;
  protected static boolean showingVertices = true;
  protected static boolean showingCoordinates = true;

  public static boolean isShowingOrientations() {
    return showingOrientations;
  }
  public static void setShowingOrientations(boolean show) {
    showingOrientations = show;
  }
  public static boolean isShowingGrid() {
    return showingGrid;
  }
  public static void setShowingGrid(boolean show) {
    showingGrid = show;
  }
  public static boolean isShowingVertices() {
    return showingVertices;
  }
  public static void setShowingVertices(boolean show) {
    showingVertices = show;
  }
  
  private PrecisionModel precisionModel = new PrecisionModel();
  private GeometryFactory geometryFactory = null;
	private GeometryEditModel geomEditModel;
  private LayerList layerList = new LayerList();
  private WKTWriter writer = new WKTWriter();
  
	public TestBuilderModel()
	{
		geomEditModel = new GeometryEditModel();
    initLayers();
    initTestCaseList();
	}
	
	public GeometryEditModel getGeometryEditModel() { return geomEditModel; }
	
	public PrecisionModel getPrecisionModel() { return precisionModel; }
	
	public void setPrecisionModel(PrecisionModel precisionModel)
	{
		this.precisionModel = precisionModel;
    geometryFactory = null;
	}
	
  public GeometryFactory getGeometryFactory()
  {
    if (geometryFactory == null)
      geometryFactory = new GeometryFactory(getPrecisionModel());
    return geometryFactory;
  }
  
  
	public String getResultDisplayString(Geometry g)
	{
		if (g == null)
			return "";
    if (g.getNumPoints() > MAX_DISPLAY_POINTS)
      return GeometryEditModel.toStringVeryLarge(g);
		return writer.writeFormatted(g);
	}
	
  public LayerList getLayers() { return layerList; }
  
  private void initLayers()
  {
    layerList.getLayer(LayerList.LYR_A).setGeometry(
        new IndexedGeometryContainer(geomEditModel, 0));
    layerList.getLayer(LayerList.LYR_B).setGeometry(
        new IndexedGeometryContainer(geomEditModel, 1));
    if (geomEditModel != null)
      layerList.getLayer(LayerList.LYR_RESULT).setGeometry(
          new ResultGeometryContainer(geomEditModel));

    Layer lyrA = layerList.getLayer(LayerList.LYR_A);
    lyrA.setStyle(new BasicStyle(GeometryDepiction.GEOM_A_LINE_CLR,
        GeometryDepiction.GEOM_A_FILL_CLR));
    
    Layer lyrB = layerList.getLayer(LayerList.LYR_B);
    lyrB.setStyle(new BasicStyle(GeometryDepiction.GEOM_B_LINE_CLR,
        GeometryDepiction.GEOM_B_FILL_CLR));
    
    Layer lyrR = layerList.getLayer(LayerList.LYR_RESULT);
    lyrR.setStyle(new BasicStyle(GeometryDepiction.GEOM_RESULT_LINE_CLR,
        GeometryDepiction.GEOM_RESULT_FILL_CLR));
  }

  public void pasteGeometry(int geomIndex)
  	throws ParseException, IOException
  {
  	Object obj = SwingUtil.getFromClipboard();
  	Geometry g = null;
  	if (obj instanceof String) {
  		g = readGeometryText((String) obj);
  	}
  	else
  		g = (Geometry) obj;
  	
    TestCaseEdit testCaseEdit = (TestCaseEdit) getCurrentTestCaseEdit();
    testCaseEdit.setGeometry(geomIndex, g);
    getGeometryEditModel().setTestCase(testCaseEdit);
  }
  
  public Geometry readGeometryText(String geomStr) 
  throws ParseException, IOException 
  {
  	MultiFormatReader reader = new MultiFormatReader(getGeometryFactory());
  	
    Geometry g = null;
    if (geomStr.length() > 0) {
      g = reader.read(geomStr);
    }
    return g;
  }

  public void loadMultipleGeometriesFromFile(int geomIndex, String filename)
  throws Exception 
  {
    Geometry g = IOUtil.readMultipleGeometriesFromFile(filename, getGeometryFactory());
    TestCaseEdit testCaseEdit = (TestCaseEdit) getCurrentTestCaseEdit();
    testCaseEdit.setGeometry(geomIndex, g);
    getGeometryEditModel().setTestCase(testCaseEdit);
  }
  
  /*
  public Geometry readMultipleGeometriesFromFile(String filename)
  throws Exception, IOException 
  {
    String ext = FileUtil.extension(filename);
    if (ext.equalsIgnoreCase("shp"))
      return readMultipleGeometriesFromShapefile(filename);
    return readMultipleGeometryFromWKT(filename);
  }
    
  private Geometry readMultipleGeometriesFromShapefile(String filename)
  throws Exception 
  {
    Shapefile shpfile = new Shapefile(new FileInputStream(filename));
    GeometryFactory geomFact = getGeometryFactory();
    shpfile.readStream(geomFact);
    List geomList = new ArrayList();
    do {
      Geometry geom = shpfile.next();
      if (geom == null)
        break;
      geomList.add(geom);
    } while (true);
    
    return geomFact.createGeometryCollection(GeometryFactory.toGeometryArray(geomList));
  }
  
  private Geometry readMultipleGeometryFromWKT(String filename)
  throws ParseException, IOException 
  {
    return readMultipleGeometryFromWKTString(FileUtil.readText(filename));
  }
  
  private Geometry readMultipleGeometryFromWKTString(String geoms)
  throws ParseException, IOException 
  {
    GeometryFactory geomFact = getGeometryFactory();
    WKTReader reader = new WKTReader(geomFact);
    WKTFileReader fileReader = new WKTFileReader(new StringReader(geoms), reader);
    List geomList = fileReader.read();
    
    if (geomList.size() == 1)
      return (Geometry) geomList.get(0);
    
    // TODO: turn polygons into a GC   
    return geomFact.buildGeometry(geomList);
  }
  
  */
  
  //=============================================================
  
  private TestCaseList tcList = new TestCaseList();
  private ListIterator tcListi;
  private List parseErrors = null;
  
  /**
   *  The current test case (if any). Invariant: currTestCase = tcListi.prev()
   */
  private TestCaseEdit currTestCase;

  public void initTestCaseList()
  {
    tcList = new TestCaseList();
    tcListi = new IteratorWrapper(tcList.getList().listIterator());
    // ensure that there is always a valid TestCase in the list
    createNew();
  }
  
  public void initList(TestCaseList tcl) {
    tcList = tcl;
    tcListi = new IteratorWrapper(tcList.getList().listIterator());
    // ensure that there is always a valid TestCase in the list
    if (tcListi.hasNext()) {
      currTestCase = (TestCaseEdit) tcListi.next();
    }
    else {
      createNew();
    }
  }

  public void createNew() {
    // move to end of list
    while (tcListi.hasNext()) {
      tcListi.next();
    }
    currTestCase = new TestCaseEdit(precisionModel);
    tcListi.add(currTestCase);
  }


  public java.util.List getTestCases() {
    return Collections.unmodifiableList(tcList.getList());
  }

  public TestCaseList getTestCaseList() {
    return tcList;
  }

  public void setCurrentTestCase(TestCaseEdit testCase) {
    while (tcListi.hasPrevious()) {
      tcListi.previous();
    }
    while (tcListi.hasNext()) {
      if (tcListi.next() == (TestCaseEdit) testCase) {
        currTestCase = testCase;
        return;
      }
    }
  }
  
  public TestCaseEdit getCurrentTestCaseEdit()
  {
    return currTestCase;
  }
  
  public Testable getCurrentTestable() {
    return currTestCase;
  }

  public int getCurrentTestIndex()
  {
    return tcListi.previousIndex();
  }
  
  public TestCaseList getTestList()
  {
    return tcList;
  }
  
  public int getTestListSize()
  {
    return tcList.getList().size();
  }
  
  public void openXmlFilesAndDirectories(File[] files) throws Exception {
     TestCaseList testCaseList = createTestCaseList(files);
    PrecisionModel precisionModel = new PrecisionModel();
    if (!testCaseList.getList().isEmpty()) {
      TestRunnerTestCaseAdapter a = (TestRunnerTestCaseAdapter) testCaseList.getList().get(0);
      precisionModel = a.getTestRunnerTestCase().getTestRun().getPrecisionModel();
    }
    if (tcList.getList().size() == 1
         && ((Testable) tcList.getList().get(0)).getGeometry(0) == null
         && ((Testable) tcList.getList().get(0)).getGeometry(1) == null) {
      loadTestCaseList(testCaseList, precisionModel);
    }
    else {
      TestCaseList newList = new TestCaseList();
      newList.add(tcList);
      newList.add(testCaseList);
      loadTestCaseList(newList, precisionModel);
    }
  }

  void loadTestCaseList(TestCaseList tcl, PrecisionModel precisionModel) throws Exception {
    setPrecisionModel(precisionModel);
    if (tcl != null) {
      loadEditList(tcl);
    }
  }

  public void loadEditList(TestCaseList tcl) 
  throws ParseException
  {
    TestCaseList newTcl = new TestCaseList();
    for (Iterator i = tcl.getList().iterator(); i.hasNext(); ) {
      Testable tc = (Testable) i.next();

        if (tc instanceof TestCaseEdit) {
          newTcl.add((TestCaseEdit) tc);
        }
        else {
          newTcl.add(new TestCaseEdit(tc));
        }

    }
    initList(newTcl);
  }

  private TestCaseList createTestCaseList(File[] filesAndDirectories) {
    TestCaseList testCaseList = new TestCaseList();
    for (int i = 0; i < filesAndDirectories.length; i++) {
      File fileOrDirectory = filesAndDirectories[i];
      if (fileOrDirectory.isFile()) {
        testCaseList.add(createTestCaseList(fileOrDirectory));
      }
      else if (fileOrDirectory.isDirectory()) {
        testCaseList.add(createTestCaseListFromDirectory(fileOrDirectory));
      }
    }
    return testCaseList;
  }

  private TestCaseList createTestCaseListFromDirectory(File directory) {
    Assert.isTrue(directory.isDirectory());
    TestCaseList testCaseList = new TestCaseList();
    List files = Arrays.asList(directory.listFiles());
    for (Iterator i = files.iterator(); i.hasNext(); ) {
      File file = (File) i.next();
      testCaseList.add(createTestCaseList(file));
    }
    return testCaseList;
  }

  private TestCaseList createTestCaseList(File xmlTestFile) 
  {
    TestReader testReader = new TestReader();
    TestRun testRun = testReader.createTestRun(xmlTestFile, 1);
    parseErrors = testReader.getParsingProblems();

    TestCaseList tcl = new TestCaseList();
    if (hasParseErrors()) {
      return tcl;
    }
    for (Iterator i = testRun.getTestCases().iterator(); i.hasNext(); ) {
      com.vividsolutions.jtstest.testrunner.TestCase testCase = (com.vividsolutions.jtstest.testrunner.TestCase) i.next();
      tcl.add(new TestRunnerTestCaseAdapter(testCase));
    }
    return tcl;
  }

  /**
   * 
   * @return empy list if no errors
   */
  public List getParsingProblems()
  {
    return parseErrors; 
  }
  
  public boolean hasParseErrors()
  {
    if (parseErrors == null) return false;
    return parseErrors.size() > 0;
  }
  
  public void prevCase() {
    // since current test case = tcListi.prev, to
    // display the case *before* the current one must move back twice
    if (tcListi.hasPrevious()) {
      tcListi.previous();
    }
    if (tcListi.hasPrevious()) {
      tcListi.previous();
    }
    currTestCase = (TestCaseEdit) tcListi.next();
  }

  public void nextCase() {
    // don't move past the last one
    if (tcListi.nextIndex() >= tcList.getList().size()) {
      return;
    }
    if (tcListi.hasNext()) {
      currTestCase = (TestCaseEdit) tcListi.next();
    }
  }

  public void copyCase() {
    TestCaseEdit copy = null;
    copy = new TestCaseEdit(currTestCase);
    tcListi.add(copy);
    currTestCase = copy;
  }

  public void addCase(Geometry[] geoms) {
    TestCaseEdit copy = null;
    copy = new TestCaseEdit(geoms);
    tcListi.add(copy);
    currTestCase = copy;
  }

  public void deleteCase() {
    tcListi.remove();
    if (tcListi.hasNext()) {
      currTestCase = (TestCaseEdit) tcListi.next();
    }
    else if (tcListi.hasPrevious()) {
      currTestCase = (TestCaseEdit) tcListi.previous();
    }
    else {
      createNew();
    }
  }

  private class IteratorWrapper implements ListIterator {
    ListIterator i;

    public IteratorWrapper(ListIterator i) {
      this.i = i;
    }

    public void set(Object o) {
      checkStop();
      i.set(o);
    }

    public boolean hasNext() {
      return i.hasNext();
    }

    public Object next() {
      checkStop();
      return i.next();
    }

    public void remove() {
      checkStop();
      i.remove();
    }

    public boolean hasPrevious() {
      return i.hasPrevious();
    }

    public Object previous() {
      checkStop();
      return i.previous();
    }

    public int nextIndex() {
      return i.nextIndex();
    }

    public int previousIndex() {
      return i.previousIndex();
    }

    public void add(Object o) {
      checkStop();
      i.add(o);
    }

    private void checkStop() {
      int a = 5;
    }
  }

}
