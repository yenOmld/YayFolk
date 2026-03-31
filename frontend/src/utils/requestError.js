export const getRequestErrorMessage = (error, options = {}) => {
  if (typeof options === 'string') {
    return error?.response?.data?.message || error?.message || options
  }

  const timeoutMessage = options?.timeoutMessage || 'Request timed out'
  const fallbackMessage = options?.fallbackMessage || 'Request failed'

  if (error?.code === 'ECONNABORTED') {
    return timeoutMessage
  }

  return error?.response?.data?.message || error?.message || fallbackMessage
}