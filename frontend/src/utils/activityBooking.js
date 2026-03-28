const DISPLAY_STATUS_MAP = {
  pending_start: {
    code: 'pending_start',
    label: '待开始',
    description: '活动还未开始，请按时到场参与。'
  },
  pending_checkin: {
    code: 'pending_checkin',
    label: '待签到/核销',
    description: '活动进行中，请现场签到或由商家核销。'
  },
  checked_in: {
    code: 'checked_in',
    label: '已签到/核销',
    description: '已完成签到或核销，等待活动结束后自动归档。'
  },
  completed: {
    code: 'completed',
    label: '已完成',
    description: '活动已结束，本次报名已完成。'
  },
  cancelled: {
    code: 'cancelled',
    label: '已取消',
    description: '该报名记录已取消，用户可直接重新报名同一活动。'
  },
  rejected: {
    code: 'rejected',
    label: '商家已拒绝',
    description: '商家已拒绝本次报名，当前不可再次申请该活动。'
  },
  missed: {
    code: 'missed',
    label: '已结束',
    description: '活动已结束，未完成签到或核销。'
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

export const isCancelableActivityBooking = (booking) => {
  return booking?.status === 'registered'
}

export const isDeletableActivityBooking = (booking) => {
  return booking?.status === 'cancelled'
}
