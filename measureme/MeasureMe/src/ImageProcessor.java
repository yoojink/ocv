import org.opencv.core.Core;
import org.opencv.core.Mat; 
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.highgui.Highgui;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.awt.Point;
import java.awt.geom.Point2D;

public class ImageProcessor {
    public static void smooth(String filename) { 
        Mat imgUp = Highgui.imread(filename, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
        Mat img = new Mat(imgUp.size(), Highgui.CV_LOAD_IMAGE_GRAYSCALE);
        Imgproc.pyrDown(imgUp, img);
        Mat black = new Mat(img.size(), Highgui.CV_LOAD_IMAGE_GRAYSCALE);
        Imgproc.threshold(img, black, 100, 255, Imgproc.THRESH_BINARY);
        Highgui.imwrite("C:\\Users\\relja\\Documents\\measureme\\images\\black.JPG", black); 
        Mat gaussian = new Mat(img.size(), Highgui.CV_LOAD_IMAGE_GRAYSCALE);
        Imgproc.GaussianBlur(img, gaussian, new Size(11, 11), 10);
        Mat mask = new Mat(img.size(), Highgui.CV_LOAD_IMAGE_GRAYSCALE);
        Mat linesImg = new Mat(img.size(), Highgui.CV_LOAD_IMAGE_GRAYSCALE);
        
        Imgproc.Canny(img, mask, 50, 200, 3, true);
        Highgui.imwrite("C:\\Users\\relja\\Documents\\measureme\\images\\out.JPG", mask);
        
        Mat imgThickened = new Mat(img.size(), Highgui.CV_LOAD_IMAGE_GRAYSCALE);
		float[] values = {0};
		int lineWidth = 2;
        for (int i = 0; i < img.rows(); i++)
        {
        	for (int j = 0; j < img.cols(); j++)
        	{
        		if (mask.get(i, j)[0] != 0)
        		{
        			int xMin = Math.max(0, i - lineWidth);
        			int xMax = Math.min(i + lineWidth, img.rows());
        			int yMin = Math.max(0, j - lineWidth);
        			int yMax = Math.min(j + lineWidth, img.cols()); 
        			for (int x = xMin; x < xMax; x++)
        			{
        				for (int y = yMin; y < yMax; y++)
        				{
        					imgThickened.put(x, y, 255);
        				}
        			}
        		}
        	}
        }
        Highgui.imwrite("C:\\Users\\relja\\Documents\\measureme\\images\\thick.JPG", imgThickened); 
        Mat lines = new Mat();
        Imgproc.HoughLinesP(imgThickened, lines, 2, Math.PI/180, 100, 100, 1);
        System.out.println("Lines size is " + lines.width() + " X " + lines.height());
        List<Line> mergedLines = mergeLines(lines);
        List<Point> points = new ArrayList<Point>();
    	System.out.println("Merged lines size is " + mergedLines.size());
        for (int i = 0; i < mergedLines.size(); i++) {
        	Line line = mergedLines.get(i).getMergedLine();
        	for (int j = i + 1; j < mergedLines.size(); j++) {
        		Line otherLine = mergedLines.get(j).getMergedLine();
        		double radians = Math.atan2(line.getSlope() - otherLine.getSlope(), 1 + line.getSlope() * otherLine.getSlope());
        		double degrees = Math.toDegrees(radians);
        		
        		if (((degrees > 60) && (degrees < 120)) || ((degrees < 300) && (degrees > 240))) {
        			double x = (otherLine.getY0() - line.getY0()) / (line.getSlope() - otherLine.getSlope());
        			double y = (otherLine.getX0() - line.getX0()) / (line.getSlope() - otherLine.getSlope());
        		}
        	}
        }
        
        Highgui.imwrite("C:\\Users\\relja\\Documents\\measureme\\images\\lines.JPG", linesImg); 
        System.out.println("Mat is " + lines.toString());
        
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(imgThickened, contours, new Mat(), Imgproc.RETR_TREE , Imgproc.CHAIN_APPROX_SIMPLE);
        System.out.println("Contours is " + contours.size() + " long");
        Mat contourMat = new Mat(img.size(), Highgui.CV_LOAD_IMAGE_COLOR);

		for(int i = 0; i < contours.size(); i++) {
			Imgproc.drawContours(contourMat, contours, i, new Scalar(255, 255, 255));
		}
        Highgui.imwrite("C:\\Users\\relja\\Documents\\measureme\\images\\contours.JPG", contourMat);
        
        List<MatOfPoint> squares = new ArrayList<MatOfPoint>();
        for (int i = 0; i < contours.size(); i++)
        {
	        MatOfPoint2f approx = new MatOfPoint2f();
	        MatOfPoint contour = contours.get(i);
	        MatOfPoint2f contour2f= new MatOfPoint2f();
	        org.opencv.core.Point[] contourPoints = contour.toArray();
	        contour2f.fromArray(contourPoints);
	        Imgproc.approxPolyDP(contour2f, approx, Imgproc.arcLength(contour2f, true) *.02, true);
	        if( (approx.elemSize() == 4) && (Math.abs(Imgproc.contourArea(approx.clone())) > 1000) && (Imgproc.isContourConvex(new MatOfPoint(approx)))) {
                double maxCosine = 0;

                /*for( int j = 2; j < 5; j++ )
                {
                    double cosine = Math.abs(angle(approx[j%4], approx[j-2], approx[j-1]));
                    maxCosine = Math.max(maxCosine, cosine);
                }

                if( maxCosine < 0.3 ) {
                    
                }*/
                squares.add(new MatOfPoint(approx));
            }
        }
        
        Mat squareImg = new Mat(img.size(), Highgui.CV_LOAD_IMAGE_GRAYSCALE);
     // the function draws all the squares in the image
        
        System.out.println("Number of squares is " + squares.size());
        Core.polylines(squareImg, squares, true, new Scalar(255,255,255));
        Highgui.imwrite("C:\\Users\\relja\\Documents\\measureme\\images\\sq.JPG", squareImg);


    }
    
    public static class Line implements Comparable<Line>
    {
    	private Point2D mStart;
    	private Point2D mEnd;
    	private double mSlope;
    	private double mY0;
    	private double mX0;
    	private double mDistance;
    	public ArrayList<Line> mMergedLines;
    	public Line(Point2D start, Point2D end) {
    		mStart = start;
    		mEnd = end;
    		mSlope = (end.getY() - start.getY()) / (end.getX() - start.getX());
    		mY0 = start.getY() - mSlope * start.getX();
    		mX0 = start.getX() - start.getY() / mSlope;
    		mDistance =  Math.sqrt(Math.pow(end.getY() - start.getY(), 2) + Math.pow(end.getX() - start.getX(), 2));
    		mMergedLines = new ArrayList<Line>();
    		mMergedLines.add(this);
    	}
    	
    	double getSlope() {
    		return mSlope;
    	}
    	
    	double getY0() {
    		return mY0;
    	}
    	
    	double getX0() {
    		return mX0;
    	}
    	
    	double getDistance() {
    		return mDistance;
    	}
    	
    	public Line getMergedLine() {
    		ArrayList<Point2D> points = new ArrayList<Point2D>((mMergedLines.size() + 1) * 2);
    		for (Line line : mMergedLines) {
        		points.add(line.mStart);
        		points.add(line.mEnd);
    		}
    		Collections.sort(points, new Comparator<Point2D>(){
    			public int compare(Point2D p1, Point2D p2) {
    				if (p1.getX() < p2.getX()) {
    					return -1;
    				}
    				if (p1.getX() > p2.getX()) {
    					return 1;
    				}
    				return 0;
    			}
    			
    			public boolean equals(Object obj) {
    				return false;
    			}
    		});
    		return new Line(points.get(0), points.get(points.size() - 1));
    	}
    	
    	void merge(Line otherLine)  {
    		double totalDistance = mDistance + otherLine.getDistance();
    		mSlope = mDistance * mSlope / totalDistance + otherLine.getDistance() * otherLine.getSlope() / totalDistance;
    		mY0 = mDistance * mY0 / totalDistance + otherLine.getDistance() * otherLine.getY0() / totalDistance;
    		mX0 = mDistance * mX0 / totalDistance + otherLine.getDistance() * otherLine.getX0() / totalDistance;
    		mDistance += otherLine.getDistance();
    		mMergedLines.addAll(otherLine.mMergedLines);
    	}
    	
    	boolean canMerge(Line otherLine) {
    		double slopeDifference = Math.abs(getSlope() - otherLine.getSlope());
    		double slopePerc = slopeDifference / getSlope();
    		if (slopePerc > MIN_PERC_SLOPE_DIFFERENCE) {
    			return false;
    		}
    		if (Math.abs(getY0()) < Math.abs(getX0())) {
	    		double y0Difference = Math.abs(getY0() - otherLine.getY0());
	    		double y0Perc = y0Difference / getY0();
	    		return y0Perc < MIN_PERC_DIFFERENCE; 
    		}
    		double x0Difference = Math.abs(getX0() - otherLine.getX0());
    		double x0Perc = x0Difference / getX0();
    		return x0Perc < MIN_PERC_DIFFERENCE; 
    	}

		@Override
		public int compareTo(Line otherLine) {
			if (getSlope() < otherLine.getSlope())
				return -1;
			if (getSlope() == otherLine.getSlope())
				return 0;
			return 1;
		}
    }
    
    private static int MIN_LINE_DISTANCE = 5; // pixels
    private static double MIN_PERC_SLOPE_DIFFERENCE = .1; //percent
    private static double MIN_PERC_DIFFERENCE = .2; //percent
    public static List<Line> mergeLines(Mat lines) {
    	List<Line> list = new ArrayList<Line>();
    	for (int i = 0; i < lines.width(); i++) {
    		Line line = new Line(new Point2D.Double(lines.get(0, i)[0], lines.get(0, i)[1]), new Point2D.Double(lines.get(0, i)[2], lines.get(0, i)[3]));
    		boolean added = false;
    		for (Line curLine : list) {
    			if (curLine.canMerge(line)) {
    				curLine.merge(line);
    				added = true;
    				break;
    			}
    		}
    		if (!added) {
	    		if (line.getDistance() > MIN_LINE_DISTANCE) {
	    			list.add(line);
	    		}
    		}
    	}
    	return list;
    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		smooth("C:\\Users\\relja\\Documents\\measureme\\images\\IMG_0383.JPG");
	}

}
