package edu.zjut.chart.plot;

import java.util.Date;
import java.util.List;

import processing.core.PApplet;
import processing.core.PConstants;
import edu.zjut.common.data.time.TimePeriod;
import edu.zjut.common.data.time.TimeSeriesCollection;
import edu.zjut.common.data.time.TimeSeriesData;

public class AreaStackedRenderer extends TimeSeriesPlot {

	float[][] sumValues;
	float[] plotX;
	float[][] plotY;
	int indexMin, indexMax;

	public AreaStackedRenderer(PApplet p, TimeSeriesCollection tsList) {
		super(p, tsList);
	}

	@Override
	protected void calcRange() {
		// 横坐标范围
		timeMin = series.getTimeMin();
		timeMax = series.getTimeMax();

		// 纵坐标范围
		int size = series.seriesSize();
		int range = series.getTimeRange();
		sumValues = new float[size][];
		for (int i = 0; i < size; i++) {
			List<Float> values = series.get(i).getValues();
			sumValues[i] = new float[range];
			for (int j = 0; j < values.size(); j++) {
				float t = 0;
				if (i > 0)
					t = sumValues[i - 1][j];

				Float value = values.get(j);
				if (value == null) {
					Float left = null;
					int leftIndex = j - 1;
					while (left == null) {
						if (leftIndex < 0)
							break;
						left = values.get(leftIndex--);
					}

					Float right = null;
					int rightIndex = j + 1;
					while (right == null) {
						if (rightIndex >= values.size())
							break;
						right = values.get(rightIndex++);
					}
					if (left == null)
						left = right;
					if (right == null)
						right = left;

					value = (left + right) / 2;
				}
				sumValues[i][j] = value + t;
			}
		}

		valueMin = Float.MAX_VALUE;
		valueMax = Float.MIN_VALUE;
		for (int i = 0; i < sumValues[size - 1].length; i++) {
			if (sumValues[size - 1][i] < valueMin)
				valueMin = sumValues[size - 1][i];
			if (sumValues[size - 1][i] > valueMax)
				valueMax = sumValues[size - 1][i];
		}
	}

	@Override
	protected void drawChart() {

		// 假设全部时间数据相同
		List<TimePeriod> years = series.get(1).getTimes();
		plotX = new float[years.size()];
		TimePeriod yearMin = visualMax, yearMax = visualMin;
		indexMin = 0;
		indexMax = years.size() - 1;
		for (int row = 0; row < years.size(); row++) {
			TimePeriod time = years.get(row);
			if (time.compareTo(visualMin) >= 0
					&& time.compareTo(visualMax) <= 0) {
				if (time.compareTo(yearMin) < 0) {
					yearMin = time;
					indexMin = row;
				}
				if (time.compareTo(yearMax) > 0) {
					yearMax = time;
					indexMax = row;
				}

				float x = PApplet.map(time.getSerialIndex(),
						visualMin.getSerialIndex(), visualMax.getSerialIndex(),
						plotX1, plotX2);
				plotX[row] = x;
			}
		}

		// 叠加后的Y绘制位置
		plotY = new float[series.seriesSize()][];
		for (int i = 0; i < series.seriesSize(); i++) {
			plotY[i] = new float[sumValues[i].length];
			for (int row = indexMin; row <= indexMax; row++) {
				float value = sumValues[i][row];
				float y = PApplet.map(value, axisValueMin, axisValueMax,
						plotY2, plotY1);
				plotY[i][row] = y;
			}
		}

		p.noStroke();
		for (int i = 0; i < series.seriesSize(); i++) {
			p.fill(colorArr[i]);

			p.beginShape();

			for (int row = indexMin; row <= indexMax; row++)
				p.vertex(plotX[row], plotY[i][row]);

			if (i == 0) {
				p.vertex(plotX[indexMax], plotY2);
				p.vertex(plotX[indexMin], plotY2);
			} else {
				for (int row = indexMax; row >= indexMin; row--)
					p.vertex(plotX[row], plotY[i - 1][row]);
			}
			p.endShape(PConstants.CLOSE);
		}
	}

	/**
	 * 高亮线, 鼠标判断
	 */
	protected void drawDataHighlight() {

		boolean find = false;
		float minDis = 5;
		int minIndex = -1, minRow = -1;
		float minX = -1, minY1 = -1, minY2 = -1;

		for (int i = 0; i < series.seriesSize(); i++) {
			TimeSeriesData ts = series.get(i);
			for (int row = indexMin; row <= indexMax; row++) {
				float dis = Math.abs(p.mouseX - plotX[row]);
				float y2 = plotY2, y1 = plotY[i][row];
				if (i > 0)
					y2 = plotY[i - 1][row];

				if (dis < minDis && (p.mouseY > y1 + y && p.mouseY < y2 + y)) {
					find = true;
					minDis = dis;
					minIndex = i;
					minRow = row;

					minX = plotX[row] + x;
					minY1 = y1 + y;
					minY2 = y2 + y;
				}
			}
		}

		if (find) {
			// 线
			p.stroke(0);
			p.strokeWeight(1.5f);

			p.line(minX - 2, minY1, minX + 2, minY1);
			p.line(minX - 2, minY2, minX + 2, minY2);

			p.line(minX, minY1, minX, minY2);

			// 文本
			Date time = series.get(minIndex).getTime(minRow).getTime();
			float value = series.get(minIndex).getValue(minRow);

			p.fill(0);
			p.textSize(12);
			p.textAlign(PConstants.LEFT);
			p.text(PApplet.nf(value, 0, 2) + " (" + dateFormat.format(time)
					+ ")", minX + 3, (minY1 + minY2) / 2);
		}
	}
}
