/**
 * 统计图表配色与通用配置
 * 供 StatsPanel 组件的「浅色（管理页右栏）」与「深色（可视化大屏）」两套主题共用
 */

// 企业类型固定配色（与管理端类型标签、类型饼图保持一致）
export const TYPE_COLORS = ['#0f9d58', '#e8a33d', '#1d6fb8', '#8e44ad']

/**
 * 省分布图的分类配色。
 * 省份数量不固定（当前 7 省，日后新增会更多），必须成对定义：
 * 深色主题下不能用深蓝这类低明度色，会糊在 #0d2440 的卡片底上。
 */
export const PROV_COLORS_LIGHT = [
  '#1d6fb8', '#0f9d58', '#e8a33d', '#8e44ad',
  '#16a6b6', '#d15b8f', '#7f8c3a', '#c0552b'
]

export const PROV_COLORS_DARK = [
  '#3aa0dc', '#3adc9a', '#f0b95c', '#b98ce0',
  '#48d6d6', '#f08cb8', '#c3d95c', '#ff9a6b'
]

export const THEMES = {
  // 管理页右栏：白底卡片
  light: {
    axisColor: '#606266',
    splitColor: '#ebeef5',
    titleColor: '#12315a',
    lineColor: '#1d6fb8',
    lineArea: 'rgba(29,111,184,0.15)',
    barColor: '#0f9d58',
    tooltip: {
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: '#c9dcf2',
      textStyle: { color: '#12315a', fontSize: 12 }
    },
    extraCss: 'border-radius:8px;box-shadow:0 4px 14px rgba(11,79,140,0.18);',
    legendColor: '#606266'
  },
  // 可视化大屏：深色底
  dark: {
    axisColor: '#8fa9c4',
    splitColor: 'rgba(143,169,196,0.18)',
    titleColor: '#dceaf7',
    lineColor: '#3aa0dc',
    lineArea: 'rgba(58,160,220,0.25)',
    barColor: '#3adc9a',
    tooltip: {
      backgroundColor: 'rgba(11,32,58,0.96)',
      borderColor: '#2a5a8c',
      textStyle: { color: '#dceaf7', fontSize: 12 }
    },
    extraCss: 'border-radius:8px;box-shadow:0 4px 18px rgba(0,0,0,0.45);',
    legendColor: '#8fa9c4'
  }
}

/** 直角坐标系（折线/柱状）的通用 tooltip */
export function axisTooltip(theme) {
  return {
    trigger: 'axis',
    confine: true,
    backgroundColor: theme.tooltip.backgroundColor,
    borderColor: theme.tooltip.borderColor,
    borderWidth: 1,
    padding: [8, 12],
    textStyle: theme.tooltip.textStyle,
    extraCssText: theme.extraCss
  }
}

/** 饼图 tooltip：展示名称 / 数量 / 占比 / 总数 */
export function pieTooltip(theme, totalOf) {
  return {
    trigger: 'item',
    confine: true,
    enterable: true,
    backgroundColor: theme.tooltip.backgroundColor,
    borderColor: theme.tooltip.borderColor,
    borderWidth: 1,
    padding: [8, 12],
    textStyle: theme.tooltip.textStyle,
    extraCssText: theme.extraCss + 'max-width:220px;white-space:normal;',
    formatter: (p) => {
      const total = (totalOf() || []).reduce((s, d) => s + Number(d.value || 0), 0) || 1
      const percent = ((Number(p.value) / total) * 100).toFixed(1)
      return `<div style="font-weight:600;margin-bottom:4px">${p.name}</div>
        <div>注册数量：<span style="font-weight:600;color:${theme.lineColor}">${p.value}</span> 家</div>
        <div>所占比例：<span style="font-weight:600;color:${theme.lineColor}">${percent}%</span>（${p.value} / ${total} 家）</div>`
    }
  }
}

/** 直角坐标系基础样式（轴线、分割线、标签颜色） */
export function axisBase(theme) {
  return {
    axisLine: { lineStyle: { color: theme.axisColor } },
    axisLabel: { color: theme.axisColor },
    splitLine: { lineStyle: { color: theme.splitColor } }
  }
}
