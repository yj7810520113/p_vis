package edu.zjut.vis.time;

import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import au.com.bytecode.opencsv.CSVReader;
import edu.zjut.common.data.time.TimePeriod;
import edu.zjut.common.data.time.TimeSeriesCollection;
import edu.zjut.common.data.time.TimeSeriesData;
import edu.zjut.common.data.time.TimeType;

public class DataLoader {

	static SimpleDateFormat format = new SimpleDateFormat("yyyy-M-d");

	static class Record implements Comparable<Record> {
		public Date date;
		public String group;
		public double value;

		public Record(Date date, String group, double value) {
			this.date = date;
			this.group = group;
			this.value = value;
		}

		@Override
		public int compareTo(Record o) {
			if (!group.equals(o.group)) {
				return group.compareTo(o.group);
			}

			if (!date.equals(o.date)) {
				return date.compareTo(o.date);
			}

			return (int) (value - o.value);
		}
	}

	/**
	 * 构建时间序列数据
	 * 
	 * @param records
	 * @return
	 */
	private static TimeSeriesData buildTimeSeries(List<Record> records,
			String curGroup, boolean avg) {
		TimeSeriesData timeSeries = new TimeSeriesData("", curGroup);

		Calendar c = Calendar.getInstance();
		List<Record> recordsGroup = null;
		int curYearMonth = -1;
		for (int i = 0; i < records.size(); i++) {
			Record record = records.get(i);
			c.setTime(record.date);
			int yearMonth = c.get(Calendar.YEAR) * 100 + c.get(Calendar.MONTH)
					+ 1;

			if (curYearMonth == -1) {
				curYearMonth = yearMonth;
				recordsGroup = new ArrayList<>();
				recordsGroup.add(record);
			} else if (curYearMonth == yearMonth) {
				recordsGroup.add(record);
			} else {
				double sum = 0;
				int count = avg ? recordsGroup.size() : 1;
				for (Record r : recordsGroup) {
					sum += r.value;
				}

				timeSeries.add(new TimePeriod(curYearMonth / 100,
						curYearMonth % 100), (float) (sum / count));

				curYearMonth = yearMonth;
				recordsGroup = new ArrayList<>();
				recordsGroup.add(record);
			}
		}

		return timeSeries;
	}

	public static TimeSeriesCollection loadDataSet(String infile, int dateCol,
			int groupCol, int valueCol, boolean avg) throws IOException,
			ParseException {

		CSVReader reader = new CSVReader(new FileReader(infile));
		List<String[]> list = reader.readAll();

		List<Record> records = new ArrayList<>();

		for (int i = 0; i < list.size(); i++) {
			String datestr = list.get(i)[dateCol];
			String group = list.get(i)[groupCol];
			String valuestr = list.get(i)[valueCol];
			Date date = format.parse(datestr);
			double value = Double.parseDouble(valuestr);
			records.add(new Record(date, group, value));
		}

		Collections.sort(records);

		List<TimeSeriesData> timeSeriesList = new ArrayList<>();
		List<Record> recordsGroup = null;
		String curGroup = null;
		for (int i = 0; i < records.size(); i++) {
			Record record = records.get(i);
			if (curGroup == null) {
				curGroup = record.group;
				recordsGroup = new ArrayList<>();
				recordsGroup.add(record);
			} else if (curGroup.equals(record.group)) {
				recordsGroup.add(record);
			} else {
				timeSeriesList
						.add(buildTimeSeries(recordsGroup, curGroup, avg));
				curGroup = record.group;
				recordsGroup = new ArrayList<>();
				recordsGroup.add(record);
			}
		}

		TimeSeriesCollection dataset = new TimeSeriesCollection(TimeType.MONTH);
		for (TimeSeriesData timeSeries : timeSeriesList) {
			dataset.addSeries(timeSeries);
		}
		return dataset;
	}

	/**
	 * 按城区划分的时间序列数据
	 * 
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	public static TimeSeriesCollection loadHZDataset(String infile,
			int dateCol, int groupCol, int valueCol, boolean avg)
			throws IOException, ParseException {

		String[] areaList = { "上城区", "下城区", "拱墅区", "西湖区", "滨江区", "江干区", "之江区",
				"下沙区" };

		// String[] areaList = { "下城区", "拱墅区", "西湖区", "滨江区", "江干区", "之江区",
		// "下沙区" };
		// String[] areaList = { "西湖区", "滨江区" };

		TimeSeriesData[] tsArr = new TimeSeriesData[areaList.length];
		for (int i = 0; i < areaList.length; i++) {
			tsArr[i] = new TimeSeriesData("", areaList[i]);
		}

		CSVReader reader = new CSVReader(new FileReader(infile));
		List<String[]> list = reader.readAll();

		TreeMap<String, TreeMap<String, Double>> priceMapList = new TreeMap<>();
		TreeMap<String, TreeMap<String, Integer>> countMapList = new TreeMap<>();
		for (int i = 0; i < areaList.length; i++) {
			priceMapList.put(areaList[i], new TreeMap<String, Double>());
			countMapList.put(areaList[i], new TreeMap<String, Integer>());
		}

		for (int i = 0; i < list.size(); i++) {
			String datestr = list.get(i)[dateCol];
			String areastr = list.get(i)[groupCol];
			String pricestr = list.get(i)[valueCol];

			java.util.Date date = format.parse(datestr);
			Calendar c = Calendar.getInstance();
			c.setTime(date);

			String month = c.get(Calendar.YEAR) + "-"
					+ (c.get(Calendar.MONTH) + 1);
			double price = Double.parseDouble(pricestr);

			Double sum = priceMapList.get(areastr).get(month);
			Integer count = countMapList.get(areastr).get(month);
			if (sum == null) {
				priceMapList.get(areastr).put(month, price);
				countMapList.get(areastr).put(month, 1);
			} else {
				priceMapList.get(areastr).put(month, price + sum);
				countMapList.get(areastr).put(month, count + 1);
			}
		}

		for (int i = 0; i < areaList.length; i++) {
			for (String month : priceMapList.get(areaList[i]).keySet()) {
				double sum = priceMapList.get(areaList[i]).get(month);
				int count = countMapList.get(areaList[i]).get(month);
				String[] toks = month.split("-");

				int y = Integer.parseInt(toks[0]);
				int m = Integer.parseInt(toks[1]);

				// 计算均价
				if (avg)
					tsArr[i].add(new TimePeriod(y, m), (float) (sum / count));
				else
					tsArr[i].add(new TimePeriod(y, m), (float) (sum));
			}
		}

		TimeSeriesCollection dataset = new TimeSeriesCollection(TimeType.MONTH);
		for (int i = 0; i < areaList.length; i++) {
			dataset.addSeries(tsArr[i]);
		}

		return dataset;
	}
}
