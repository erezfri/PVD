package com.example.erezfri.pvd;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.view.View;

/**
 * @author Eliyahu Sason
 * This class plot some Dynamic graphs in a scorll mode like a scope.
 * - fixed y range [mMaxy,mMaxy] is assumed.
 * - when u add data u need to specify the graph numer
 * - u can define horizontal axes length, change the graph colors, add grid and legend.   
 */

public class PlotDynamic extends View{
	private Paint mPaintAxes,mPaintText;
	private ArrayList<Paint> mPaint;
	private int mPlotNum;
	private String mTitle=null;
	private String[] mPlotName=null;
	private boolean mGrid=false;
	private int mylinesNum=5;
	
	private int[] mColorConstants={Color.RED,Color.GREEN,Color.CYAN,Color.MAGENTA};
	private int mColorGrid;
	private int mColorBackgroud;
	public enum Colorpart{plots,grid,backgroud};
	
	private ArrayList<ArrayList<Integer>> yvaluesGroup;
	private ArrayList<ArrayList<Float>> xvaluesGroup;
		
	private float mMaxy=50,mMiny=-50,mPixels;
	private float mxInterval =10;
	private Canvas mCanvas;
	private float mcanvasHeight,mcanvasWidth;
	private int initiate = 0;
	
	
	/**
	 * PlotDynamic's Constructor - Creates and initializes PlotDynamic object
	 * @param context 
	 * @param plotNum - number of plots - must be natural number
	 * @param xInterval - the horizontal axes length - must be real and positive number 
	 */
	public PlotDynamic(Context context,int plotNum, float xInterval) {
		super(context);
		mPlotNum=plotNum;
		yvaluesGroup=new ArrayList<ArrayList<Integer>>(mPlotNum);
		xvaluesGroup=new ArrayList<ArrayList<Float>>(mPlotNum);
		mPaint =new ArrayList<Paint>(mPlotNum);
				
		for (int i=0;i<mPlotNum;i++){
			yvaluesGroup.add(new ArrayList<Integer>());	
			xvaluesGroup.add(new ArrayList<Float>());
			
			Paint paint = new Paint();
			paint.setStrokeWidth(1);
			paint.setColor(mColorConstants[i]);
			paint.setTextSize(20.0f);
			
			mPaint.add(paint);		
		}
		
		mPaintAxes=new Paint();
		mPaintAxes.setStrokeWidth(2);
		
		mPaintText=new Paint();
		mPaintText.setColor(Color.BLACK);
		
		mxInterval=xInterval;
	}

	/**
	 * Called when the view should render its content. 
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		mCanvas=canvas;
		
		if (initiate == 0){
			mcanvasHeight = canvas.getHeight();
			mcanvasWidth = canvas.getWidth();
			mPixels=mcanvasHeight;
			initiate = 1;
		}
		   
		canvas.drawColor(mColorBackgroud);
		
		setAxes();
		setGrid();
		Legend();
		setTitle();
		
	    int[][] xvaluesInPixels = new int[mPlotNum][];
	    for (int i=0;i<mPlotNum;i++){
	    	//xvaluesInPixels[i]=toPixel(mcanvasWidth*2,xvaluesGroup.get(i));//TODO here
			xvaluesInPixels[i]=toPixel(mcanvasWidth*2,xvaluesGroup.get(i));
	    }
	    
		for (int i=0;i<mPlotNum;i++){	
			ArrayList<Integer> yvalues = yvaluesGroup.get(i);
			if (yvalues.size()>1){
				int[] xvalues = xvaluesInPixels[i];
				//Paint paint = mPaint.get(i);
				Paint paint = new Paint();
				paint.setColor(Color.BLUE);
				paint.setStrokeWidth(5.0f);

				for (int j = 0; j < yvalues.size()-1; j++) {
					//canvas.drawCircle(xvalues[j],mcanvasHeight-yvalues.get(j).intValue(),2,paint);
					//canvas.drawCircle(xvalues[j+1],mcanvasHeight-yvalues.get(j+1).intValue(),2,paint);
					canvas.drawLine(xvalues[j],mcanvasHeight-yvalues.get(j).intValue(),xvalues[j+1],mcanvasHeight-yvalues.get(j+1).intValue(),paint);
				}
			}
			xvaluesGroup.get(i).clear();
			yvaluesGroup.get(i).clear();
		}	
	}
	
	/**
	 * AddData add one point to one of the plots.
	 * u need to specify x and y values and a valid plot index.
	 */
	public void addData(float x,float y,int plotInd){
		if (Float.isNaN(x) || Float.isNaN(y)) return;
		ArrayList<Integer> yvalues = yvaluesGroup.get(plotInd);  
		ArrayList<Float> xvalues = xvaluesGroup.get(plotInd);


		yvalues.add(Integer.valueOf(toPixel(y)));	// add y value as pixel
		xvalues.add(Float.valueOf(x));

		

	}
	
	
	/**
	 * Set a legend to the graph
	 * u need to specify an array of plot names with length equals to the plot number
	 */
	public void Legend(String[] plotName){
		mPlotName=plotName;
	}
	private void Legend(){
		if (mPlotName!=null){
			double[] posTitles = new double[]{0.75*mcanvasWidth,0.13*mcanvasHeight};

			double[] posTitlesSign = new double[]{(0.75-0.03)*mcanvasWidth,(0.13-0.005)*mcanvasHeight};
		
			double deltaTitle=0.03*mcanvasHeight;
			double[] deltaTitleSign=new double[]{0.015*mcanvasWidth,deltaTitle};
			mPaintText.setTextSize(15.0f);
			mPaintText.setTextAlign(Align.LEFT);
		
			for(int i=0;i<mPlotNum;i++){
				mCanvas.drawText(mPlotName[i], (float)posTitles[0],(float)(i*deltaTitle+posTitles[1]), mPaintText);
				//mCanvas.drawCircle((float)posTitlesSign[0], (float)(posTitlesSign[1]+i*deltaTitleSign[1]), 3, mPaint.get(i));
				mCanvas.drawLine((float)(posTitlesSign[0]-deltaTitleSign[0]), (float)(posTitlesSign[1]+i*deltaTitleSign[1]), (float)(posTitlesSign[0]+deltaTitleSign[0]), (float)(posTitlesSign[1]+i*deltaTitleSign[1]),  mPaint.get(i));	
				}
			float delta = mcanvasHeight/(mylinesNum+1);
			float deltaTime=delta/mcanvasWidth*mxInterval;
			mCanvas.drawText(("Time segment is: "+String.format("%.2f", deltaTime)), (float)posTitles[0],(float)(mPlotNum*deltaTitle+posTitles[1]), mPaintText);
			
			}
		
		
		}
	
	/**
	 * Sets the main title of the graph
	 * u need to specify the title.
	 */
	public void setTitle(String title){
		mTitle=title;
	}
	private void setTitle(){
		if(mTitle!=null){
			mPaintText.setTextSize(20.0f);
			mPaintText.setTextAlign(Align.CENTER);
			mCanvas.drawText(mTitle, (float)0.5*mcanvasWidth,(float)0.1*mcanvasHeight, mPaintText);
		}
	}
	
	/**
	 * Sets the grid on or off accordingly to the GridOn variable
	 * true mean on, false mean off.
	 */
	public void setGrid(boolean GridOn){
		mGrid=GridOn;
	}
	private void setGrid(){
		if(mGrid){
			mylinesNum=5;//odd
			float delta = mcanvasHeight/(mylinesNum+1);
			int xlinesNum=(int)((0.9*mcanvasWidth)/delta);
			mPaintAxes.setColor(mColorGrid);
			//plot y grid lines except the x axes
			for (int i=1;i<=mylinesNum;i++){
				if(i==mylinesNum/2+1) continue; // the line in the middle is the 0 line
				mCanvas.drawLine((float)0.1*mcanvasWidth,i*delta,mcanvasWidth,i*delta,mPaintAxes);		
			}
			//plot x grid lines
			for(int i=1;i<=xlinesNum;i++){
				mCanvas.drawLine((float)(0.1*mcanvasWidth+i*delta),0,(float)(0.1*mcanvasWidth+i*delta),mcanvasHeight,mPaintAxes);
			}
		}
	}
	
	
	/**
	 * Sets the Axes of the graph and put values besides y-axes's grid lines 
	 */
	private void setAxes(){
		//plot Axes
		mPaintAxes.setColor(Color.BLACK);
		mCanvas.drawLine(0, mcanvasHeight / 2, mcanvasWidth, mcanvasHeight / 2, mPaintAxes);
		mCanvas.drawLine((float)0.1*mcanvasWidth,0,(float)0.1*mcanvasWidth,mcanvasHeight,mPaintAxes);
		//mCanvas.drawLine((float)mcanvasWidth,0,(float)mcanvasWidth,mcanvasHeight,mPaintAxes);
		//Put values
		mPaintText.setTextSize(20.0f);
		mPaintText.setTextAlign(Align.RIGHT);
		float deltaVal = (mMaxy-mMiny)/(mylinesNum+1);//delta in values
		float deltaPixel = mcanvasHeight/(mylinesNum+1);
		
		for (int i=1;i<=mylinesNum;i++){
			//if(i==mylinesNum/2+1) continue;
			mCanvas.drawText(String.format("%.2f", (mMaxy-deltaVal*i) ), (float)(0.1-0.015)*mcanvasWidth, (float)(i*deltaPixel+0.01*mcanvasHeight), mPaintText);	
		}
	}
	
	/**
	 * Set the plots' colors to the different parts of the graph. 
	 * @param colorArray - an array of colors with length determined according 
	 *        to the object u fix it's color  
	 * @param which - the part u fix it's color
	 */
	public void setColor(String[] colorArray,Colorpart which){
		switch (which){
		case plots:
			for (int i=0;i<mPlotNum;i++){
				String colors= colorArray[i];
				int color=Color.parseColor(colors);
				mPaint.get(i).setColor(color);	
		}
			break;
		case grid:
			mColorGrid = Color.parseColor(colorArray[0]);
			
			break;
		case backgroud:
			mColorBackgroud = Color.parseColor(colorArray[0]);
			break;
		}
	}
	
	/**
	 * Inner function. Converts from one value to pixel.used for the y axes.  
	 */
	private int toPixel(float value){
		double p;
		float halfpixels=mPixels/2;
		
		if(value>0){
			//p = halfpixels+(value/mMaxy)*.9*halfpixels;
			p = halfpixels+(value/mMaxy)*halfpixels;
		}
		else if(value<0){
			//p = halfpixels-(value/mMiny)*.9*halfpixels;
			p = halfpixels-(value/mMiny)*halfpixels;
		}
		else {
			p = halfpixels;
		}
		return (int)p;
	}
	
	/**
	 * Convent x array of values to array of pixels
	 */
	private int[] toPixel(float pixels, ArrayList<Float> values) {
	int[] pointsInt=new int[values.size()];
	int i=0;
	double p;
	double startpixel=0.05*pixels;
	if (values.size()>1){
		float min = values.get(0);
		
		for(Float f: values){
			p = startpixel+((f.floatValue() - min)/mxInterval)*.9*pixels;
			pointsInt[i] = (int)p;
			i++;
		}

		}
	return (pointsInt);
}

	
}

