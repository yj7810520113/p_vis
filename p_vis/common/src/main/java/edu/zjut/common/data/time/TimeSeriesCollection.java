package edu.zjut.common.data.time;

import java.util.ArrayList;
import java.util.List;

public class TimeSeriesCollection {

	private TimeType type;
	private List<TimeSeriesData> dataList;

	private TimePeriod timeMin;
	private TimePeriod timeMax;

	float valueMin, valueMax;

	/**
	 * 由最小时间和最大时间填充而成
	 */
	private List<TimePeriod> allTimes;

	public TimeSeriesCollection(TimeType type) {
		this.type = type;
		dataList = new ArrayList<TimeSeriesData>();
	}

	public void addSeries(TimeSeriesData series) {
		dataList.add(series);
	}

	public void removeSeries(TimeSeriesData series) {
		dataList.remove(series);
	}

	/**
	 * 多个时间序列数据整合. 由于时间分布并不完全一致, 最小时间, 最大时间, 缺失值
	 */
	public void buildTimeSeries() {

		timeMin = dataList.get(0).getTimeMin();
		timeMax = dataList.get(0).getTimeMax();
		for (TimeSeriesData series : dataList) {
			TimePeriod min = series.getTimeMin();
			TimePeriod max = series.getTimeMax();
			if (timeMin.compareTo(min) > 0)
				timeMin = min;
			if (timeMax.compareTo(max) < 0)
				timeMax = max;
		}

		for (TimeSeriesData series : dataList) {
			series.fillTimeRange(timeMin, timeMax, type);
		}

		allTimes = new ArrayList<TimePeriod>();
		for (TimePeriod time = timeMin; time.compareTo(timeMax) <= 0; time = time
				.rollDate(type, 1)) {
			allTimes.add(time);
		}

		// 纵坐标范围
		valueMin = Float.MAX_VALUE;
		valueMax = Float.MIN_VALUE;
		for (TimeSeriesData series : dataList) {
			List<Float> values = series.getValues();
			for (float value : values) {
				if (value < valueMin)
					valueMin = value;
				if (value > valueMax)
					valueMax = value;
			}
		}
	}

	public TimePeriod getTimeMin() {
		return timeMin;
	}

	public TimePeriod getTimeMax() {
		return timeMax;
	}

	public float getValueMin() {
		return valueMin;
	}

	public float getValueMax() {
		return valueMax;
	}

	public TimeSeriesData get(int i) {
		return dataList.get(i);
	}

	public int size() {
		return dataList.size();
	}

	public List<TimePeriod> getAllTimes() {
		return allTimes;
	}

	public TimePeriod getTime(int index) {
		return allTimes.get(index);
	}

	public TimeType getTimeType() {
		return type;
	}

	public String[] getNames() {
		String[] names = new String[dataList.size()];
		for (int i = 0; i < dataList.size(); i++) {
			names[i] = dataList.get(i).getValueName();
		}

		return names;
	}
}
