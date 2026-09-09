import _ from 'lodash';

export function initChart() {
  return {
    xAxis: {
      type: 'category',
      data: [],
    },
    yAxis: [
      {
        type: 'value',
      },
    ],
    legend: {
      data: ['value'],
    },
    tooltip: {
      trigger: 'axis',
    },
    dataZoom: [
      {
        type: 'inside',
        start: 80,
        end: 100,
      },
      {
        start: 80,
        end: 100,
      },
    ],
    series: [],
  };
}

export function initData(): {
  x: string[];
  y: number[][];
} {
  return {
    x: [],
    y: [[], [], []],
  };
}

/** 原始指标数据点，tags 为 JSON 字符串 */
interface MetricRawItem {
  timeKey: string;
  tags: string;
  /** 0: counter, 1: gauge, 3: timer */
  type: number;
  value: number;
  max?: number;
  total?: number;
}

interface AnalyzedItem {
  timeKey: string;
  tags: string;
  type: number;
  value: number;
  valueBaisc?: number;
  max?: number;
  totalBaisc?: number;
  total?: number;
}

type Accuracy = 'minutes' | 'hours' | 'days';

export function doAnalysis({
  data,
  poly,
  step = 'minutes',
}: {
  data: MetricRawItem[];
  poly?: Record<string, string>;
  step?: Accuracy;
}) {
  const steps: Record<Accuracy, number> = {
    minutes: 60,
    hours: 60 * 60,
    days: 24 * 60 * 60,
  };
  // 聚合
  const polyGroupBy = _.groupBy(data, k => k.timeKey);
  const polyData: AnalyzedItem[] = [];
  for (let i = 0; i < Object.keys(polyGroupBy).length; i++) {
    let item: MetricRawItem[] = polyGroupBy[Object.keys(polyGroupBy)[i]];
    if (poly) {
      item = _.filter(item, obj => {
        const tags: Record<string, string> = JSON.parse(obj.tags);
        for (const k of Object.keys(poly)) {
          if (tags[k] !== poly[k]) {
            return false;
          }
        }
        return true;
      });
    }
    let res: AnalyzedItem | undefined;
    switch (item[0]?.type) {
      case 0: {
        const valueBaisc =
          _.reduce(item, (sum, n) => sum + n.value, 0) / item.length;
        res = {
          timeKey: item[0].timeKey,
          tags: item[0].tags,
          type: item[0].type,
          valueBaisc,
          value:
            (valueBaisc - (polyData[i - 1]?.valueBaisc || 0)) / steps[step],
        };
        break;
      }
      case 1: {
        res = {
          timeKey: item[0].timeKey,
          tags: item[0].tags,
          type: item[0].type,
          value: _.reduce(item, (sum, n) => sum + n.value, 0) / item.length,
          max: _.reduce(item, (max, n) => Math.max(max, n.max ?? 0), 0),
        };
        break;
      }
      case 3: {
        const totalBaisc = _.reduce(
          item,
          (sum, n) => sum + (n.total as number),
          0
        );
        res = {
          timeKey: item[0].timeKey,
          tags: item[0].tags,
          type: item[0].type,
          value: _.reduce(item, (sum, n) => sum + n.value, 0) / item.length,
          max: _.reduce(item, (max, n) => Math.max(max, n.max ?? 0), 0),
          totalBaisc,
          total:
            (totalBaisc - (polyData[i - 1]?.totalBaisc || 0)) / steps[step],
        };
        break;
      }
      default:
        break;
    }
    if (res) {
      polyData.push(res);
    }
  }
  // 数据类型分组
  const metricKind: Record<number, string> = {
    0: 'counter',
    1: 'gauge',
    3: 'timer',
  };
  const groupBy = _.groupBy(_.drop(polyData), k => metricKind[k.type]);
  const result = initData();
  // 遍历所有分组
  Object.keys(groupBy).map(k => {
    // counter
    if (k === 'counter') {
      _.forEach(groupBy[k], item => {
        result.x.push(item.timeKey);
        result.y[0].push(item.value);
      });
    }
    // gauge
    if (k === 'gauge') {
      _.forEach(groupBy[k], item => {
        result.x.push(item.timeKey);
        result.y[0].push(item.value);
        result.y[1].push(item.max ?? 0);
      });
    }
    // timer
    if (k === 'timer') {
      _.forEach(groupBy[k], item => {
        result.x.push(item.timeKey);
        result.y[0].push(item.value);
        result.y[1].push(item.max ?? 0);
        result.y[2].push(item.total ?? 0);
      });
    }
    return true;
  });

  return result;
}
