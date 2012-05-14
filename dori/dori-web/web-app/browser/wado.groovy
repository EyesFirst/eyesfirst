/*
HTTP PROXY CONNECTIONS
 */
import javax.servlet.http.*
import javax.servlet.ServletContext

HttpServletRequest request = request;
HttpServletResponse response = response;
ServletContext context = context;
private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());

final wadoURL = context.getInitParameter('wadoURL') ?: grails.util.GrailsConfig['eyesfirst.wadoURL'];
assert wadoURL;

final bufferSize = 1024*2024;
//final ignoreHeaders = ['Keep-Alive', 'Connection', 'Transfer-Encoding'] as Set
final keepHeaders = ['Accept','Accept-Encoding','Cache-Control','Content-Type','If-Modified-Since',
    'Pragma','Cache-Control','Content-Encoding','Content-Length','Last-Modified'] as Set
final attachmentFilename = request.getParameter("filename") ?: "dicom.dcm";

//--set up target connection
final url = new URL(wadoURL+"?"+request.getQueryString());
final urlCon = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);

urlCon.setDoOutput(false);
urlCon.setDoInput(true);
for (hName in request.headerNames) {
  //if (!ignoreHeaders.contains(hName))
  if (keepHeaders.contains(hName))
    urlCon.setRequestProperty(hName,request.getHeader(hName));
}

urlCon.connect();

//--set response headers
response.setStatus(urlCon.responseCode);

for(entry in urlCon.getHeaderFields()) {//name-values[]
  if (keepHeaders.contains(entry.key)) {
    entry.value.each {
        log.debug("adding header k:$entry.key v:$it")
        response.addHeader(entry.key,it)
    }
  }
}
response.addHeader('Via','EyesFirst wado proxy')
if ( (urlCon.responseCode / 100 as int) in [2,3]) {
  response.setHeader("Content-Disposition","attachment; filename="+attachmentFilename);
  response.setDateHeader("Expires",(new Date()+1).time)

  //--stream output
  response.outputStream.withStream { OutputStream responseOutputStream ->
    urlCon.inputStream.withStream { InputStream targetInput ->
      byte[] bytes = new byte[bufferSize];
      while(true) {
        int read = targetInput.read(bytes);
        if (read == -1)
          break;
        responseOutputStream.write(bytes,0,read);
      }
      responseOutputStream.flush();
    }
  }
} else {
  String msg = "Couldn't fetch URL (status:$response.status) $url"
  log.warn msg
  //println msg
}
urlCon.disconnect();
