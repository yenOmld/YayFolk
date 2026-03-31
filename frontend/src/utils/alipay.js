export const isAlipayPagePayment = (payload) => payload?.paymentMode === 'alipay_page' && typeof payload?.formHtml === 'string'

export const submitAlipayForm = (formHtml) => {
  if (!formHtml) {
    throw new Error('Missing Alipay payment form')
  }

  document.open()
  document.write(formHtml)
  document.close()
}
