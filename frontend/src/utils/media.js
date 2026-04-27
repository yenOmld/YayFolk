const VIDEO_EXTENSIONS = [
  '.mp4',
  '.webm',
  '.ogg',
  '.mov',
  '.m4v',
  '.mkv',
  '.avi',
  '.flv',
  '.3gp'
]

export function isVideoUrl(url) {
  if (!url || typeof url !== 'string') {
    return false
  }

  const normalized = url.trim().split('?')[0].split('#')[0].toLowerCase()
  if (!normalized) {
    return false
  }

  if (normalized.startsWith('blob:') || normalized.startsWith('data:video/')) {
    return true
  }

  return VIDEO_EXTENSIONS.some(extension => normalized.endsWith(extension))
}

export function normalizeMediaList(value) {
  if (Array.isArray(value)) {
    return value.map(item => (typeof item === 'string' ? item.trim() : item)).filter(Boolean)
  }

  if (typeof value === 'string' && value.trim()) {
    try {
      const parsed = JSON.parse(value)
      if (Array.isArray(parsed)) {
        return parsed.map(item => (typeof item === 'string' ? item.trim() : item)).filter(Boolean)
      }
    } catch (error) {
      return [value.trim()].filter(Boolean)
    }
  }

  return []
}

export function normalizeImageList(value) {
  return normalizeMediaList(value).filter(item => !isVideoUrl(item))
}

export function pickPrimaryMediaUrl(value, fallback = '') {
  const list = normalizeMediaList(value)
  return list.find(item => !isVideoUrl(item)) || list[0] || fallback
}

export function buildMediaItems(value, poster = '') {
  const list = normalizeMediaList(value)
  const imageItems = list.map((url, index) => ({
    id: `image-${index}`,
    type: isVideoUrl(url) ? 'video' : 'image',
    url,
    poster,
    label: isVideoUrl(url) ? 'Video' : `Image ${index + 1}`
  }))

  return imageItems
}
