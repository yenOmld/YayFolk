/**
 * 七牛云 / OSS 图片缩略图 URL 工具
 * 为列表/头像等小尺寸场景自动拼接缩略图参数，减少图片传输大小。
 */

const OSS_DOMAINS = ['yayfolk.bhyy.online', 'qiniu', 'oss', 'img']

function isOssUrl(url) {
  if (!url || typeof url !== 'string') return false
  return OSS_DOMAINS.some(domain => url.includes(domain))
}

/**
 * 生成缩略图 URL
 * @param {string} url   原始图片 URL
 * @param {number} width 期望宽度（px），七牛云 imageView2 会等比缩放
 * @returns {string} 处理后的 URL
 */
export function thumbnailUrl(url, width = 400) {
  if (!url) return url
  if (!isOssUrl(url)) return url
  // 已带处理参数则不再拼接
  if (url.includes('imageView2') || url.includes('imageMogr2')) return url
  const sep = url.includes('?') ? '&' : '?'
  return `${url}${sep}imageView2/2/w/${width}`
}

/**
 * 头像缩略图（小尺寸）
 */
export function avatarThumb(url) {
  return thumbnailUrl(url, 100)
}

/**
 * 列表卡片缩略图
 */
export function cardThumb(url) {
  return thumbnailUrl(url, 400)
}
