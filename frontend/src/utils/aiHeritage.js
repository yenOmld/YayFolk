// AI辅助生成：豆包API（Doubao-Seedream-5.0-lite），2025-04-24
export const AI_HERITAGE_DRAFT_KEY = 'yayfolk-ai-heritage-draft'
export const AI_HERITAGE_RETURN_KEY = 'yayfolk-ai-heritage-return-to'
export const AI_HERITAGE_SOURCE_FOLDER = 'ai-heritage/source'
export const AI_HERITAGE_MODEL_NAME = '豆包 Seedream 5.0'

let aiHeritageSourceFile = null
let aiHeritageSourcePreviewUrl = ''

export const heritageThemes = [
  { id: 'opera', name: '京剧脸谱', shortDesc: '戏台气韵、红金张力', icon: 'bx bx-mask', gradient: 'linear-gradient(135deg, #8d2323, #c2410c)' },
  { id: 'cloisonne', name: '掐丝珐琅', shortDesc: '蓝金质感、精致华丽', icon: 'bx bx-gift', gradient: 'linear-gradient(135deg, #0f766e, #14b8a6)' },
  { id: 'embroidery', name: '苏绣', shortDesc: '细密丝线、柔和雅致', icon: 'bx bx-needle', gradient: 'linear-gradient(135deg, #7c3aed, #a855f7)' },
  { id: 'kite', name: '风筝扎制', shortDesc: '轻盈飘逸、春日感', icon: 'bx bx-wind', gradient: 'linear-gradient(135deg, #0ea5e9, #38bdf8)' },
  { id: 'shadow', name: '皮影戏', shortDesc: '光影叙事、民间故事感', icon: 'bx bx-movie-play', gradient: 'linear-gradient(135deg, #b45309, #f59e0b)' },
  { id: 'woodblock', name: '木版年画', shortDesc: '年味色彩、热闹喜庆', icon: 'bx bx-grid-alt', gradient: 'linear-gradient(135deg, #dc2626, #f59e0b)' },
  { id: 'paper-cut', name: '剪纸', shortDesc: '镂空层次、窗花意趣', icon: 'bx bx-cut', gradient: 'linear-gradient(135deg, #db2777, #fb7185)' },
  { id: 'lacquer', name: '漆器', shortDesc: '黑红光泽、沉稳高级', icon: 'bx bx-collection', gradient: 'linear-gradient(135deg, #1f2937, #64748b)' },
  { id: 'brocade', name: '蜀锦', shortDesc: '织锦纹样、华美细节', icon: 'bx bx-table', gradient: 'linear-gradient(135deg, #a16207, #fbbf24)' }
]

export const scenePresets = [
  { id: 'floating-display', name: '居中展示', brief: '主体居中，简单大方', prompt: '主体居中摆放，前后层次轻微分开，留出干净呼吸感，让成品像被认真摆拍的展示照。' },
  { id: 'editorial-desk', name: '桌面陈列', brief: '像桌面静物，亲切自然', prompt: '像桌面静物拍摄一样安排构图，少量纸张、卡片、纹理和辅助道具围绕主体，营造自然又有设计感的成品展示。' },
  { id: 'window-light', name: '窗边柔光', brief: '柔和窗光，清爽耐看', prompt: '使用窗边柔光、轻微反射和干净背景，让主体看起来明亮、通透、很适合分享。' },
  { id: 'dream-stage', name: '梦幻舞台', brief: '带一点舞台氛围', prompt: '背景带有轻微光晕、弥散雾感和舞台感层次，像在梦幻舞台上展示精致成品。' },
  { id: 'gallery-corner', name: '展厅角落', brief: '像小展厅里的角落', prompt: '像小型展厅的一角，墙面、地面和投影形成安静、克制但有质感的展示空间，适合突出主体细节。' },
  { id: 'shelf-stage', name: '层架陈列', brief: '台座和层架，秩序感强', prompt: '把主体放进有层架、台座和秩序感的陈列空间里，画面清晰、稳妥，像精品展示区。' }
]

export const posterStylePresets = [
  {
    id: 'sticker-party',
    name: '贴纸派对',
    shortDesc: '活泼贴纸感，轻松又亮眼',
    icon: 'bx bx-sticker',
    gradient: 'linear-gradient(145deg, #ff8a65, #ffcf6f)',
    heritageThemeId: 'paper-cut',
    heritageTitle: '贴纸派对',
    heritagePrompt: '把参考图里的成品当作唯一主角，背景做成像手帐贴纸墙一样的展示氛围，加入圆角贴纸、胶带、星星、小图标和明快色块，整体轻松、有趣、年轻，突出成品本身的轮廓和细节，不要替换主体。',
    prompt: '背景要有手帐和贴纸墙的感觉，用明快色块、圆角贴纸、胶带边、星星和小装饰营造轻松有趣的氛围。主体保持原图成品，不要换成别的东西，主体清楚、背景热闹但不抢戏。'
  },
  {
    id: 'collage-notes',
    name: '拼贴手作',
    shortDesc: '撕纸拼贴，手作感很强',
    icon: 'bx bx-layer',
    gradient: 'linear-gradient(145deg, #7c8cff, #b58cff)',
    heritageThemeId: 'lacquer',
    heritageTitle: '拼贴手作',
    heritagePrompt: '把参考图里的成品保留为画面中心，背景做成拼贴手作风格，像是撕边纸张、便签、票据、布纹和纸片层叠出来的展示底板，整体有手工感、层次感和一点小惊喜，但主体不能被改掉。',
    prompt: '背景像手作拼贴板一样，把撕边纸、便签、票据、布纹和纸片层层叠起来，做出温暖、有趣、很会摆拍的展示感。主体继续保持原图成品，不要替换，不要加新的主角。'
  },
  {
    id: 'magazine-cover',
    name: '杂志封面',
    shortDesc: '大留白，拍出来很上镜',
    icon: 'bx bx-book-content',
    gradient: 'linear-gradient(145deg, #6b7280, #d1d5db)',
    heritageThemeId: 'woodblock',
    heritageTitle: '杂志封面',
    heritagePrompt: '把参考图里的成品保留为主角，背景做成杂志封面式的干净版面，留白充足、分区清晰、线条利落、光影高级，整体像时尚杂志的成品展示页，不要生成真实文字内容。',
    prompt: '背景要像杂志封面一样干净利落，有大片留白、简洁分区、细致光影和高级质感。主体保持原图成品，画面要显得上镜、清楚、舒服，不要乱加文字。'
  },
  {
    id: 'window-display',
    name: '橱窗展陈',
    shortDesc: '像店里陈列，精致又明亮',
    icon: 'bx bx-window-alt',
    gradient: 'linear-gradient(145deg, #9dd6c6, #f3c8a2)',
    heritageThemeId: 'kite',
    heritageTitle: '橱窗展陈',
    heritagePrompt: '把参考图里的成品保留为展示橱窗里的主角，背景像精品店或展柜陈列一样明亮、干净、有秩序，加入柔光、玻璃感、细台座和轻微反射，让成品看起来像被认真摆放的展品。',
    prompt: '背景像精品店橱窗或展柜陈列，明亮、干净、有秩序，加入柔光、玻璃感、台座和轻微反射，让成品像被认真展示的展品。主体保持原图，不要换成别的东西。'
  },
  {
    id: 'neon-show',
    name: '霓虹秀场',
    shortDesc: '亮一点，更有舞台感',
    icon: 'bx bx-glow',
    gradient: 'linear-gradient(145deg, #7a5cff, #ff6aa6)',
    heritageThemeId: 'cloisonne',
    heritageTitle: '霓虹秀场',
    heritagePrompt: '把参考图里的成品保留在画面中心，背景加入柔和霓虹、渐变光晕、舞台灯和一点点未来感，但整体仍然要优雅克制，突出成品的轮廓和质感，不要让背景压过主体。',
    prompt: '背景带一点舞台和霓虹感，用柔和光晕、渐变、灯带和轻微发光做出热闹但不吵的氛围。主体保持原图成品，画面要显眼、好看、有一点点舞台感。'
  },
  {
    id: 'luxury-gift',
    name: '华丽礼盒',
    shortDesc: '金边绸面，精致又贵气',
    icon: 'bx bx-gift',
    gradient: 'linear-gradient(145deg, #b0894f, #f4d58d)',
    heritageThemeId: 'brocade',
    heritageTitle: '华丽礼盒',
    heritagePrompt: '把参考图里的成品保留下来，背景做成像高级礼盒或庆典包装一样的华丽展示空间，加入金边、绸面、细腻纹理、柔亮高光和深浅层次，让成品看起来更精致、更有收藏感，但不要把主体改成别的物体。',
    prompt: '背景像高级礼盒或庆典包装，加入金边、绸面、细腻纹理和柔亮高光，做出精致、隆重、很适合送礼的展示感。主体保持原图成品，背景华丽但不杂乱。'
  }
]

export const posterSizePresets = [
  {
    id: 'adaptive',
    name: '原图比例',
    shortDesc: '跟着原图走',
    size: '2k',
    aspect: '原图比例',
    prompt: '优先保持参考图原有比例和构图，适合想尽量贴近原图展示效果的场景。'
  },
  {
    id: 'landscape-classic',
    name: '标准横图',
    shortDesc: '适合海报和画册',
    size: '2k',
    aspect: '3:2',
    prompt: '使用 3:2 的标准横图构图，适合海报展示、画册排版和常见横屏浏览。'
  },
  {
    id: 'landscape-cinema',
    name: '宽屏横图',
    shortDesc: '像电影封面',
    size: '3k',
    aspect: '16:9',
    prompt: '使用 16:9 的宽屏构图，适合封面、大屏展示和更有电影感的横版画面。'
  },
  {
    id: 'landscape-wide',
    name: '超宽横图',
    shortDesc: '更有张力',
    size: '3k',
    aspect: '2:1',
    prompt: '使用 2:1 的超宽构图，适合横向延展的展示画面，能做出更强的空间感和张力。'
  },
  {
    id: 'landscape-ultra',
    name: '手机竖图',
    shortDesc: '适合竖屏分享',
    size: '4k',
    aspect: '9:16',
    prompt: '使用 9:16 的竖屏构图，适合手机社交分享和竖版海报展示。'
  }
]

export const presetTags = ['AI非遗大片', '非遗国风', '横版海报', '电影感', '传统美学', '项目主题']

export const defaultAiHeritageDraft = () => ({
  sourceImageUrl: '',
  theme: posterStylePresets[0].heritageThemeId,
  scene: scenePresets[0].id,
  posterStyle: posterStylePresets[0].id,
  imageSize: posterSizePresets[1].id,
  title: '',
  content: '',
  tags: ['AI非遗大片', '非遗国风'],
  generatedImageUrl: ''
})

export function readAiHeritageDraft() {
  if (typeof window === 'undefined') return defaultAiHeritageDraft()
  const raw = window.sessionStorage.getItem(AI_HERITAGE_DRAFT_KEY)
  if (!raw) return defaultAiHeritageDraft()
  try {
    return { ...defaultAiHeritageDraft(), ...JSON.parse(raw) }
  } catch (error) {
    console.error('Failed to parse AI heritage draft', error)
    return defaultAiHeritageDraft()
  }
}

export function writeAiHeritageDraft(patch) {
  if (typeof window === 'undefined') return defaultAiHeritageDraft()
  const nextDraft = { ...readAiHeritageDraft(), ...patch }
  window.sessionStorage.setItem(AI_HERITAGE_DRAFT_KEY, JSON.stringify(nextDraft))
  return nextDraft
}

export function clearAiHeritageDraft() {
  if (typeof window === 'undefined') return
  window.sessionStorage.removeItem(AI_HERITAGE_DRAFT_KEY)
}

export function setAiHeritageReturnPath(path) {
  if (typeof window === 'undefined') return
  if (path) {
    window.sessionStorage.setItem(AI_HERITAGE_RETURN_KEY, path)
  }
}

export function getAiHeritageReturnPath() {
  if (typeof window === 'undefined') return ''
  return window.sessionStorage.getItem(AI_HERITAGE_RETURN_KEY) || ''
}

export function clearAiHeritageReturnPath() {
  if (typeof window === 'undefined') return
  window.sessionStorage.removeItem(AI_HERITAGE_RETURN_KEY)
}

export function setAiHeritageSourceFile(file) {
  clearAiHeritageSourceFile()
  if (!file) return null

  aiHeritageSourceFile = file
  aiHeritageSourcePreviewUrl = URL.createObjectURL(file)
  return aiHeritageSourcePreviewUrl
}

export function getAiHeritageSourceFile() {
  return aiHeritageSourceFile
}

export function getAiHeritageSourcePreviewUrl() {
  return aiHeritageSourcePreviewUrl
}

export function clearAiHeritageSourceFile() {
  if (typeof window === 'undefined') {
    aiHeritageSourceFile = null
    aiHeritageSourcePreviewUrl = ''
    return
  }
  if (aiHeritageSourcePreviewUrl) {
    URL.revokeObjectURL(aiHeritageSourcePreviewUrl)
  }
  aiHeritageSourceFile = null
  aiHeritageSourcePreviewUrl = ''
}

export function getThemeById(themeId) {
  return heritageThemes.find(item => item.id === themeId) || heritageThemes[0]
}

export function getSceneById(sceneId) {
  return scenePresets.find(item => item.id === sceneId) || scenePresets[0]
}

export function getPosterStyleById(styleId) {
  return posterStylePresets.find(item => item.id === styleId) || posterStylePresets[0]
}

export function getPosterSizeById(sizeId) {
  return posterSizePresets.find(item => item.id === sizeId) || posterSizePresets[0]
}

export function normalizePosterSizeRequestValue(sizeValue) {
  const normalized = String(sizeValue || '').trim().toLowerCase()
  if (!normalized || normalized === 'adaptive') return '2k'
  if (normalized === '2k' || normalized === '3k' || normalized === '4k') return normalized
  if (/^\d{3,4}x\d{3,4}$/.test(normalized)) return normalized
  return '2k'
}

export function buildHeritagePrompt(themeId, sceneId, styleId, sizeId) {
  const scene = getSceneById(sceneId)
  const style = getPosterStyleById(styleId)
  const theme = getThemeById(themeId || style.heritageThemeId)
  const size = getPosterSizeById(sizeId)

  return [
    '请根据参考图生成一张成品展示图，画面的重点是已经完成的作品本身。',
    '先识别并保留参考图中的成品主体，主体不替换、不变形、不新增别的主角，只重绘背景和展示氛围。',
    '主体要求清晰、完整、边缘干净，尽量保留原本轮廓、颜色、材质和细节。',
    `背景风格：${style.name}。${style.prompt}`,
    `构图方式：${scene.name}。${scene.prompt}`,
    `画面比例：${size.name}（${size.aspect}）。${size.prompt}`,
    `主题气质：${theme.name}。${theme.shortDesc}`,
    '背景可以加入少量纸感、光影、台座、纹理、装饰元素或陈列道具，但不要抢主体，不要生成新的主体，也不要把主体改成别的物件。',
    '整体风格要好看、干净、吸睛，像适合发社交平台的成品展示照，不能有文字、水印、边框。'
  ].join('\n')
}