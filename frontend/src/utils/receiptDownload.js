export function isSuccessfulPaymentStatus(status) {
  return status === 'COMPLETED' || status === 'SUCCESS';
}

export function saveReceiptPdf(response, paymentId) {
  const blob = response?.data instanceof Blob
	? response.data
	: new Blob([response?.data], { type: 'application/pdf' });

  const downloadUrl = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = downloadUrl;
  link.download = extractFilename(response?.headers?.['content-disposition'], paymentId);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(downloadUrl);
}

export async function extractReceiptDownloadError(error, fallbackMessage = 'Unable to download receipt') {
  const responseData = error?.response?.data;

  if (responseData instanceof Blob) {
	try {
	  const text = await responseData.text();
	  const parsed = JSON.parse(text);
	  if (parsed?.message) {
		return parsed.message;
	  }
	  if (text) {
		return text;
	  }
	} catch {
	  // Ignore blob parsing errors and fall back to the default message below.
	}
  }

  return error?.response?.data?.message || error?.message || fallbackMessage;
}

function extractFilename(contentDisposition, paymentId) {
  const match = /filename\s*=\s*"?([^";]+)"?/i.exec(contentDisposition || '');
  return match?.[1] || `payment-receipt-${paymentId}.pdf`;
}

