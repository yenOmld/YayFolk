const DISPLAY_STATUS_MAP = {
  pending_start: {
    code: 'pending_start',
    label: '即将开始',
    description: '报名已确认。请准时到达活动地点。'
  },
  pending_checkin: {
    code: 'pending_checkin',
    label: '待核销',
    description: '活动进行中。打开二维码让商家验证。'
  },
  checked_in: {
    code: 'checked_in',
    label: '已核销',
    description: '该报名已核销成功。'
  },
  completed: {
    code: 'completed',
    label: '已完成',
    description: '活动已结束，该报名已完成。'
  },
  cancelled: {
    code: 'cancelled',
    label: '已取消',
    description: '该报名已取消。'
  },
  rejected: {
    code: 'rejected',
    label: '已拒绝',
    description: '商家拒绝了这个报名。'
  },
  missed: {
    code: 'missed',
    label: '未核销',
    description: '活动已结束，该报名未被核销。'
  }
}

const parseDate = (value) => {
  if (!value) {
    return null
  }

  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

export const resolveActivityBookingDisplayStatus = (booking) => {
  if (!booking) {
    return DISPLAY_STATUS_MAP.pending_start
  }

  const now = new Date()
  const startTime = parseDate(booking.startTime)
  const endTime = parseDate(booking.endTime)
  const rawStatus = booking.status

  if (rawStatus === 'cancelled') {
    return DISPLAY_STATUS_MAP.cancelled
  }

  if (rawStatus === 'rejected') {
    return DISPLAY_STATUS_MAP.rejected
  }

  if (rawStatus === 'checked_in') {
    if (endTime && now >= endTime) {
      return DISPLAY_STATUS_MAP.completed
    }
    return DISPLAY_STATUS_MAP.checked_in
  }

  if (endTime && now >= endTime) {
    return DISPLAY_STATUS_MAP.missed
  }

  if (startTime && now < startTime) {
    return DISPLAY_STATUS_MAP.pending_start
  }

  return DISPLAY_STATUS_MAP.pending_checkin
}

export const isCancelableActivityBooking = (booking) => booking?.status === 'registered'

export const isDeletableActivityBooking = (booking) => booking?.status === 'cancelled'

export const isPayableActivityBooking = (booking) => (
  booking?.status === 'registered'
  && booking?.paymentStatus === 'unpaid'
  && Number(booking?.payAmount || 0) > 0
)

export const canOpenActivityCheckin = (booking) => (
  !['cancelled', 'rejected'].includes(booking?.status)
  && booking?.paymentStatus === 'paid'
)

export const hasReviewedActivityBooking = (booking) => Boolean(
  booking?.reviewId
  || booking?.reviewTime
  || booking?.reviewScore
  || booking?.reviewContent
)

export const isReviewableActivityBooking = (booking) => {
  const status = resolveActivityBookingDisplayStatus(booking).code
  return !hasReviewedActivityBooking(booking) && (status === 'checked_in' || status === 'completed')
}
