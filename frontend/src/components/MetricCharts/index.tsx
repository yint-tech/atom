import { useContext, useEffect, useState } from 'react';
import ReactEcharts from 'echarts-for-react';
import clsx from 'clsx';
import { AppContext } from '../../adapter';
import { createUseStyles } from 'react-jss';
import { useTheme } from '../../common/theme';
import type { EChart4MQLData } from '../../types/api';

type EchartSeries = EChart4MQLData['series'];

/** echarts tooltip formatter 的回调参数（本图表只用到这几个字段） */
interface TooltipFormatterParam {
  marker: string;
  seriesName: string;
  value: string | number;
  axisValue: string;
}

const buildEchartOption = (
  title: string | undefined,
  legends: string[],
  x: string[],
  series: EchartSeries,
  bottomLegend?: boolean
) => {
  for (let i = 0; i < series.length; i++) {
    for (let j = 0; j < series[i].data.length; j++) {
      if (series[i].data[j] > 1) {
        series[i].data[j] = parseFloat(series[i].data[j].toFixed(2));
      }
    }
  }

  return {
    title: {
      left: 'center',
      text: title,
    },
    tooltip: {
      // 内容过多时将 tooltips 以滚动条的形式展示。
      trigger: 'axis',
      axisPointer: {
        // 坐标轴指示器，坐标轴触发有效
        type: 'shadow', // 默认为直线，可选为：'line' | 'shadow'
      },
      enterable: true, // 鼠标是否可进入提示框浮层中
      hideDelay: 200, // 浮层隐藏的延迟
      confine: true,
      backgroundColor: 'rgba(255,255,255, 1)',
      formatter: function (params: TooltipFormatterParam[]) {
        var htmlStr =
          '<div style="height: auto;max-height: 240px;overflow-y: auto;"><p>' +
          params[0].axisValue +
          '</p>';
        for (var i = 0; i < params.length; i++) {
          htmlStr +=
            '<p style="color: #666;">' +
            params[i].marker +
            params[i].seriesName +
            ':' +
            params[i].value +
            '</p>';
        }
        htmlStr += '</div>';
        return htmlStr;
      },
      extraCssText: 'box-shadow: 0 0 3px rgba(150,150,150, 0.7);',
      textStyle: {
        fontSize: 12,
        color: '#fff',
        align: 'left',
      },
    },
    legend: bottomLegend
      ? {
          orient: 'horizontal',
          x: 'center',
          top: '65%',
          data: legends,
        }
      : {
          orient: 'vertical',
          left: 'right',
          bottom: 'middle',
          data: legends,
        },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: x,
    },
    dataZoom: [
      {
        start: 0,
        end: 100,
      },
    ],
    yAxis: {
      type: 'value',
    },
    grid: bottomLegend
      ? {
          bottom: '35%',
          containLabel: true,
        }
      : {},
    series: series,
  };
};

const useStyles = createUseStyles({
  root: {
    textAlign: 'center',
  },
});

interface MetricChartProps {
  height?: string;
  title?: string;
  mql?: string;
  accuracy?: string;
  onLoadMsg?: (msg: string) => void;
  bottomLegend?: boolean;
  className?: string;
}

const MetricChart = (props: MetricChartProps) => {
  const {
    height = props.bottomLegend ? '350px' : '300px',
    title,
    mql,
    accuracy,
    onLoadMsg,
    bottomLegend,
    className,
  } = props;
  const { api } = useContext(AppContext);
  const theme = useTheme();
  const classes = useStyles({ theme });
  const [echartOption, setEchartOption] = useState(
    buildEchartOption(title, [], [], [], bottomLegend)
  );

  useEffect(() => {
    if (!mql || !accuracy) {
      return;
    }
    api
      .mqlQuery({
        mqlScript: mql,
        accuracy: accuracy,
      })
      .then(res => {
        if (res.status === 0) {
          setEchartOption(
            buildEchartOption(
              title,
              res.data!['legends'],
              res.data!['xaxis'],
              res.data!['series'],
              bottomLegend
            )
          );
        }
        if (onLoadMsg) {
          onLoadMsg(res.message!);
        }
      });
  }, [bottomLegend, onLoadMsg, mql, accuracy, title, api]);

  return (
    <div className={clsx(classes.root, className)}>
      <ReactEcharts
        notMerge={true}
        option={echartOption}
        style={{ width: '100%', height }}
      />
    </div>
  );
};

export default MetricChart;
