package replicatorg.app.ui;

import java.awt.GraphicsConfiguration;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.Enumeration;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingBox;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Switch;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.swing.JFrame;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import org.j3d.renderer.java3d.loaders.STLLoader;

import com.sun.j3d.loaders.IncorrectFormatException;
import com.sun.j3d.loaders.ParsingErrorException;
import com.sun.j3d.loaders.Scene;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.universe.SimpleUniverse;

public class STLFrame extends JFrame {

	BoundingSphere bounds =
		new BoundingSphere(new Point3d(0.0,0.0,0.0), 100.0);

	public STLFrame(String path) {
		setTitle(path);
		setSize(400,400);
		/**
		 * Creates new form HelloUniverse
		 */
		// Initialize the GUI components
		initComponents();

		// Create Canvas3D and SimpleUniverse; add canvas to drawing panel
		Canvas3D c = createUniverse();
		drawingPanel.add(c, java.awt.BorderLayout.CENTER);

		// Create the content branch and add it to the universe
		scene = createSTLScene(path);
		univ.addBranchGraph(scene);

		c.addKeyListener( new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyChar() == 'a') {
					cameraTranslation.x += 0.05;
				} else if (e.getKeyChar() == 'z') {
					cameraTranslation.x -= 0.05;
				} else if (e.getKeyChar() == 's') {
					cameraTranslation.y += 0.05;
				} else if (e.getKeyChar() == 'x') {
					cameraTranslation.y -= 0.05;
				} else if (e.getKeyChar() == 'd') {
					cameraTranslation.z += 0.05;
				} else if (e.getKeyChar() == 'c') {
					cameraTranslation.z -= 0.05;
				} else if (e.getKeyChar() == '[') {
					elevationAngle += 0.05;
				} else if (e.getKeyChar() == ']') {
					elevationAngle -= 0.05;
				} else if (e.getKeyChar() == '{') {
					turntableAngle += 0.05;
				} else if (e.getKeyChar() == '}') {
					turntableAngle -= 0.05;
				} else if (e.getKeyChar() == 'e') {
					if (showEdges) {
						objectSwitch.setWhichChild(0);
						showEdges = false;
					} else {
						objectSwitch.setWhichChild(1);
						showEdges = true;
					}
				} else {
					return;
				}
				updateVP();
			}
			
			public void keyReleased(KeyEvent e) {
			}
			
			public void keyTyped(KeyEvent e) {
			}
		});
	}
	
	private SimpleUniverse univ = null;
	private BranchGroup scene = null;
	/**
	 * The switch object that allows us to toggle between wireframe and solid modes.
	 */
	private Switch objectSwitch = null;
	/**
	 * Indicates whether we're in edge (wireframe) mode.  False indicates a solid view. 
	 */
	private boolean showEdges = false;
	/**
	 * The transform group for the shape.  The enclosed transform should be applied to the shape before:
	 * * bounding box calculation
	 * * saving out the STL for skeining
	 */
	private TransformGroup shapeTransform = new TransformGroup();
	
	public Node makeAmbientLight() {
		AmbientLight ambient = new AmbientLight();
		ambient.setColor(new Color3f(0.3f,0.3f,0.3f));
		ambient.setInfluencingBounds(bounds);
		return ambient;
	}

	public Node makeDirectedLight() {
		Color3f color = new Color3f(0.7f,0.7f,0.7f);
		Vector3f direction = new Vector3f(1f,0.7f,-0.2f);
		DirectionalLight light = new DirectionalLight(color,direction);
		light.setInfluencingBounds(bounds);
		return light;
	}

	final double wireBoxCoordinates[] = {
			 0,  0,  0,    0,  0,  1,
			 0,  1,  0,    0,  1,  1,
			 1,  1,  0,    1,  1,  1,
			 1,  0,  0,    1,  0,  1,
			 
			 0,  0,  0,    0,  1,  0,
			 0,  0,  1,    0,  1,  1,
			 1,  0,  1,    1,  1,  1,
			 1,  0,  0,    1,  1,  0,

			 0,  0,  0,    1,  0,  0,
			 0,  0,  1,    1,  0,  1,
			 0,  1,  1,    1,  1,  1,
			 0,  1,  0,    1,  1,  0,
	};

	public Shape3D makeBoxFrame(Point3d ll, Vector3d dim) {
		Appearance edges = new Appearance();
		edges.setLineAttributes(new LineAttributes(1,LineAttributes.PATTERN_DOT,true));
		edges.setPolygonAttributes(new PolygonAttributes(PolygonAttributes.POLYGON_LINE,
				PolygonAttributes.CULL_NONE,0));
		double[] coords = new double[wireBoxCoordinates.length];
		for (int i = 0; i < wireBoxCoordinates.length;) {
			coords[i] = (wireBoxCoordinates[i] * dim.x) + ll.x; i++;
			coords[i] = (wireBoxCoordinates[i] * dim.y) + ll.y; i++;
			coords[i] = (wireBoxCoordinates[i] * dim.z) + ll.z; i++;
		}
		LineArray wires = new LineArray(wireBoxCoordinates.length/3,GeometryArray.COORDINATES);
		wires.setCoordinates(0, coords);

		return new Shape3D(wires,edges); 
	}
	
	public Node makeBoundingBox() {

		Shape3D boxframe = makeBoxFrame(new Point3d(-50,-50,-50), new Vector3d(100,100,100));	
		
		Appearance sides = new Appearance();
		sides.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.NICEST,0.9f));
		Color3f color = new Color3f(0.05f,0.05f,1.0f); 
		Material m = new Material(color,color,color,color,64.0f);
		sides.setMaterial(m);

		Box box = new Box(50,50,50,sides);
		Transform3D tf = new Transform3D();
		tf.setTranslation(new Vector3d(0,0,50));
		TransformGroup tg = new TransformGroup(tf);
		tg.addChild(box);
		tg.addChild(boxframe);
		return tg;
	}
	
	public Node makeBackground() {
		Background bg = new Background(0.5f,0.5f,0.6f);
		bg.setApplicationBounds(bounds);
		return bg;
	}
	
	public Node makeBaseGrid() {
		Appearance edges = new Appearance();
		edges.setLineAttributes(new LineAttributes(1,LineAttributes.PATTERN_DOT,true));
		edges.setColoringAttributes(new ColoringAttributes(0.7f,0.7f,1f,ColoringAttributes.FASTEST));
		final int LINES = 11;
		LineArray grid = new LineArray(4*LINES,GeometryArray.COORDINATES);
		for (int i = 0; i < LINES; i++) {
			double offset = -50 + (100/(LINES-1))*i;
			int idx = i*4;
			// Along x axis
			grid.setCoordinate(idx++, new Point3d(offset,-50,0));
			grid.setCoordinate(idx++, new Point3d(offset,50,0));
			// Along y axis
			grid.setCoordinate(idx++, new Point3d(-50,offset,0));
			grid.setCoordinate(idx++, new Point3d(50,offset,0));
		}
		return new Shape3D(grid,edges); 
	}

	private BoundingBox getBoundingBox(Shape3D shape) {
		BoundingBox bb = null;
		Enumeration geometries = shape.getAllGeometries();
		while (geometries.hasMoreElements()) {
			Geometry g = (Geometry)geometries.nextElement();
			if (g instanceof GeometryArray) {
				GeometryArray ga = (GeometryArray)g;
				Point3d p = new Point3d();
				for (int i = 0; i < ga.getVertexCount(); i++) {
					ga.getCoordinate(i,p);
					if (bb == null) { bb = new BoundingBox(p,p); }
					bb.combine(p);
				}
			}
		}
		return bb;
	}
	
	private Node makeShape(String path) {
		STLLoader loader = new STLLoader();
		Scene scene = null;
		try {
			scene = loader.load((new File(path)).toURI().toURL());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IncorrectFormatException e) {
			e.printStackTrace();
		} catch (ParsingErrorException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		if (scene == null) { return null; }
		
		BranchGroup sourceGroup = scene.getSceneGroup();

		objectSwitch = new Switch();
		Shape3D originalShape = (Shape3D)sourceGroup.getChild(0);

		Shape3D shape = (Shape3D)originalShape.cloneTree();
		Shape3D edgeClone = (Shape3D)originalShape.cloneTree();
		objectSwitch.addChild(shape);
		objectSwitch.addChild(edgeClone);
		objectSwitch.setWhichChild(0);
		objectSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		
		Color3f color = new Color3f(0.05f,1.0f,0.04f); 
		Material m = new Material();
		//m.setAmbientColor(color);
		m.setDiffuseColor(color);
		//m.setSpecularColor(new Color3f(1f,1f,1f));
		Appearance solid = new Appearance();
		solid.setMaterial(m);
		//solid.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.NICEST, 0.2f));
		shape.setAppearance(solid);
		
		Appearance edges = new Appearance();
		edges.setLineAttributes(new LineAttributes(1,LineAttributes.PATTERN_SOLID,true));
		edges.setPolygonAttributes(new PolygonAttributes(PolygonAttributes.POLYGON_LINE,
				PolygonAttributes.CULL_NONE,0));
		edgeClone.setAppearance(edges);
		
		return objectSwitch;
	}

	/*
	 * 		Point3d lower = new Point3d();
		Point3d upper = new Point3d();		
		BoundingBox bb = getBoundingBox(originalShape);
		bb.getLower(lower);
		bb.getUpper(upper);
		Vector3d size = new Vector3d();
		size.sub(upper, lower);
		Shape3D bounds = makeBoxFrame(lower, size);
*/
	public BranchGroup createSTLScene(String path) {
		// Create the root of the branch graph
		BranchGroup objRoot = new BranchGroup();

		// Create the TransformGroup node and initialize it to the
		// identity. Enable the TRANSFORM_WRITE capability so that
		// our behavior code can modify it at run time. Add it to
		// the root of the subgraph.
		TransformGroup objTrans = new TransformGroup();
		objTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		// All sizes are represented in mm.  We scale this down so that 1mm == 0.01 units.
		Transform3D scaleTf = new Transform3D();
		scaleTf.setScale(0.01d);
		objTrans.setTransform(scaleTf);
		objRoot.addChild(objTrans);

		System.err.println(path);
		BranchGroup sceneGroup = new BranchGroup();			
		sceneGroup.addChild(makeShape(path));
		sceneGroup.addChild(makeAmbientLight());
		sceneGroup.addChild(makeDirectedLight());
		sceneGroup.addChild(makeBoundingBox());
		sceneGroup.addChild(makeBackground());
		sceneGroup.addChild(makeBaseGrid());
			
		objTrans.addChild(sceneGroup);

		// Create a new Behavior object that will perform the
		// desired operation on the specified transform and add
		// it into the scene graph.
		//	Transform3D yAxis = new Transform3D();
		//	Alpha rotationAlpha = new Alpha(-1, 4000);

		//	RotationInterpolator rotator =
		//	    new RotationInterpolator(rotationAlpha, objTrans, yAxis,
		//				     0.0f, (float) Math.PI*2.0f);
		//	BoundingSphere bounds =
		//	    new BoundingSphere(new Point3d(0.0,0.0,0.0), 100.0);
		//	rotator.setSchedulingBounds(bounds);
		objRoot.compile();

		return objRoot;
	}

	// These values were determined experimentally to look pretty dang good.
	Vector3d cameraTranslation = new Vector3d(0,0.4,2.5);
	double elevationAngle = 1.278;
	double turntableAngle = 0.214;
	
	private void updateVP() {
		TransformGroup viewTG = univ.getViewingPlatform().getViewPlatformTransform();
		Transform3D t3d = new Transform3D();
		Transform3D trans = new Transform3D();
		Transform3D rotZ = new Transform3D();
		Transform3D rotX = new Transform3D();
		trans.setTranslation(cameraTranslation);
		rotX.rotX(elevationAngle);
		rotZ.rotZ(turntableAngle);
		t3d.mul(rotZ);
		t3d.mul(rotX);
		t3d.mul(trans);
		viewTG.setTransform(t3d);
	}

	private Canvas3D createUniverse() {
		// Get the preferred graphics configuration for the default screen
		GraphicsConfiguration config =
			SimpleUniverse.getPreferredConfiguration();

		// Create a Canvas3D using the preferred configuration
		Canvas3D c = new Canvas3D(config);

		// Create simple universe with view branch
		univ = new SimpleUniverse(c);
		univ.getViewer().getView().setSceneAntialiasingEnable(true);
		updateVP();
		
		// Ensure at least 5 msec per frame (i.e., < 200Hz)
		univ.getViewer().getView().setMinimumFrameCycleTime(5);

		return c;
	}

	private void initComponents() {
		drawingPanel = new javax.swing.JPanel();

		drawingPanel.setLayout(new java.awt.BorderLayout());

		drawingPanel.setPreferredSize(new java.awt.Dimension(400, 400));
		getContentPane().add(drawingPanel, java.awt.BorderLayout.CENTER);

		pack();
	}


	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JPanel drawingPanel;
	// End of variables declaration//GEN-END:variables


}
