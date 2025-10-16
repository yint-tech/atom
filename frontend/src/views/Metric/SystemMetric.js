import React from 'react';
import { useTranslation } from 'react-i18next';

import MetricPage from './MetricPage';

const SystemMetrics = () => {
  const { t } = useTranslation();
  
  const systemMQL = [
    {
      title: t('metrics.looperThreadDashboard'),
      mql: `# ${t('metrics.looperThread')}
looperTaskQueueSize = metric(looper.taskQueueSize);
${t('metrics.looperThreadBacklog')} = aggregate(looperTaskQueueSize,'serverId','name');

looperTime = metric(looper.time);
${t('metrics.thread')} = aggregate(looperTime,'serverId','name');
show(${t('metrics.looperThreadBacklog')},${t('metrics.thread')});
        `,
    },
    {
      title: t('metrics.threadPoolDashboard'),
      bottomLegend: true,
      mql: `
        
# ${t('metrics.threadPool')}
threadPoolCount = metric(thread.pool.count);
threadPoolCount = aggregate(threadPoolCount,'serverId','name');
${t('metrics.taskReject')} = threadPoolCount[type='reject']
${t('metrics.taskException')} = threadPoolCount[type='exception']
show(${t('metrics.taskReject')},${t('metrics.taskException')});

threadPoolGauge =  metric(thread.pool.gauge)
threadPoolGauge = aggregate(threadPoolGauge,'serverId','name');
${t('metrics.coreThreadCount')} = threadPoolGauge[type='coreSize']
${t('metrics.activeCount')} = threadPoolGauge[type='activeCount']
${t('metrics.queueTaskCount')} = threadPoolGauge[type='queueSize']
show(${t('metrics.coreThreadCount')},${t('metrics.activeCount')},${t('metrics.queueTaskCount')})

threadPoolTime =  metric(thread.pool.timer)
threadPoolTime = aggregate(threadPoolTime,'serverId','name');
${t('metrics.taskExecutionCount')} = threadPoolTime[timer_type='count']
${t('metrics.maxTaskTime')} =  threadPoolTime[timer_type='max']
${t('metrics.avgTaskTime')} =  threadPoolTime[timer_type='time'] / ${t('metrics.taskExecutionCount')}
show(${t('metrics.taskExecutionCount')},${t('metrics.maxTaskTime')},${t('metrics.avgTaskTime')})
        `,
    },
    {
      title: t('metrics.disk'),
      mql: `
# ${t('metrics.disk')}
${t('metrics.availableSpace')} = metric(disk.free);
${t('metrics.totalSpace')} = metric(disk.total);

${t('metrics.availableSpace')} = aggregate(${t('metrics.availableSpace')},'serverId','path'); 
${t('metrics.totalSpace')} = aggregate(${t('metrics.totalSpace')},'serverId','path')

${t('metrics.diskUsage')} = ((${t('metrics.totalSpace')} - ${t('metrics.availableSpace')}) * 100) / ${t('metrics.totalSpace')};

${t('metrics.availableSpace')} = ${t('metrics.availableSpace')}/1048576; 
${t('metrics.totalSpace')} = ${t('metrics.totalSpace')}/1048576; 

show(${t('metrics.availableSpace')},${t('metrics.totalSpace')},${t('metrics.diskUsage')});

        `,
    },
    {
      title: t('metrics.loadTitle'),
      mql: `
${t('metrics.serverMinuteLoad')} = metric(system.load.average.1m);
${t('metrics.serverMinuteLoad')} = aggregate(${t('metrics.serverMinuteLoad')},'serverId')
show(${t('metrics.serverMinuteLoad')});

${t('metrics.systemCpuUsage')} = metric(system.cpu.usage);
${t('metrics.systemCpuUsage')} = aggregate(${t('metrics.systemCpuUsage')},'serverId')
${t('metrics.processCpuUsage')} = metric(process.cpu.usage);
${t('metrics.processCpuUsage')} = aggregate(${t('metrics.processCpuUsage')},'serverId')
show(${t('metrics.systemCpuUsage')},${t('metrics.processCpuUsage')});
        `,
    },
    {
      title: t('metrics.jvmMemory'),
      bottomLegend: true,
      mql: `
        ${t('metrics.jvmMemory')} = metric(jvm.memory.used);
${t('metrics.jvmMemory')} = aggregate(${t('metrics.jvmMemory')},'area','serverId');
show(${t('metrics.jvmMemory')});
        `,
    },
  ];

  return (
    <div>
      <p>
        {t('metrics.systemMetricsNote')}
      </p>
      <MetricPage configs={systemMQL} />
    </div>
  );
};

export default SystemMetrics;
