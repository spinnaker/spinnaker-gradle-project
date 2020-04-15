package com.netflix.spinnaker.gradle.publishing.bintray

import com.netflix.gradle.plugins.deb.Deb
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

class BintrayDebUploadTask extends DefaultTask {

  @Inject
  BintrayDebUploadTask(Deb deb) {
    super()
    this.deb = deb
    dependsOn(deb)
  }

  private Deb deb

  @TaskAction
  void uploadDeb() {
    def extension = project.extensions.getByType(BintrayPublishExtension)
    def uri = extension.getDebPublishUri(deb)
    def file = deb.getArchiveFile().get().asFile
    def contentLength = file.size()
    project.logger.info("Uploading $file to $uri")
    HttpURLConnection con = (HttpURLConnection) extension
      .getDebPublishUri(deb)
      .toURL()
      .openConnection()
    con.doOutput = true
    con.requestMethod = 'PUT'
    con.addRequestProperty('Authorization', extension.getBasicAuthHeader())
    con.setRequestProperty('Content-Type', 'application/octet-stream')
    con.setRequestProperty('Content-Length', "$contentLength")
    con.getOutputStream().withCloseable { OutputStream os ->
      file.newInputStream().withCloseable { InputStream is ->
        byte[] buf = new byte[16 * 1024]
        int bytesRead
        while ((bytesRead = is.read(buf)) != -1) {
          os.write(buf, 0, bytesRead)
        }
        os.flush()
        project.logger.info("upload complete")
      }
    }
    project.logger.info("Waiting for HTTP response")
    int httpStatus = con.responseCode
    project.logger.info("Upload finished with status $httpStatus: ${con.responseMessage}")
    (httpStatus >= 400 ?
      con.getErrorStream() :
      con.getInputStream()).withCloseable { InputStream is ->
      project.logger.debug("Upload response:\n$is.text")
    }
    con.disconnect()
  }
}
