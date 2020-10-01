package com.netflix.spinnaker.gradle.publishing.artifactregistry;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.artifactregistry.v1beta1.model.Operation;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.cloud.artifactregistry.auth.DefaultCredentialProvider;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ArtifactRegistryDebPublishTask extends DefaultTask {

  private static final Logger logger =
      LoggerFactory.getLogger(ArtifactRegistryDebPublishTask.class);

  private Provider<String> uploadBucket;
  private Provider<String> repoProject;
  private Provider<String> location;
  private Provider<String> repository;
  private Provider<RegularFile> archiveFile;

  @Inject
  public ArtifactRegistryDebPublishTask() {}

  @Input
  public Provider<String> getUploadBucket() {
    return uploadBucket;
  }

  public void setUploadBucket(Provider<String> uploadBucket) {
    this.uploadBucket = uploadBucket;
  }

  @Input
  public Provider<String> getRepoProject() {
    return repoProject;
  }

  public void setRepoProject(Provider<String> repoProject) {
    this.repoProject = repoProject;
  }

  @Input
  public Provider<String> getLocation() {
    return location;
  }

  public void setLocation(Provider<String> location) {
    this.location = location;
  }

  @Input
  public Provider<String> getRepository() {
    return repository;
  }

  public void setRepository(Provider<String> repository) {
    this.repository = repository;
  }

  @InputFile
  public Provider<RegularFile> getArchiveFile() {
    return archiveFile;
  }

  public void setArchiveFile(Provider<RegularFile> archiveFile) {
    this.archiveFile = archiveFile;
  }

  @TaskAction
  void publishDeb() throws GeneralSecurityException, InterruptedException, IOException {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    BlobId blobId = uploadDebToGcs(storage);
    Operation importOperation = importDebToArtifactRegistry(blobId);

    deleteDebFromGcs(storage, blobId);

    if (importOperation.getResponse().get("errors") != null) {
      throw new IOException(
          "Received an error importing debian package to Artifact Registry: "
              + importOperation.getResponse().get("errors"));
    }
  }

  private void deleteDebFromGcs(Storage storage, BlobId blobId) {
    try {
      storage.delete(blobId);
    } catch (StorageException e) {
      logger.warn("Error deleting deb from temp GCS storage", e);
    }
  }

  @NotNull
  private Operation importDebToArtifactRegistry(BlobId blobId)
      throws GeneralSecurityException, IOException, InterruptedException {

    ArtifactRegistryAlphaClient artifactRegistryClient =
        new ArtifactRegistryAlphaClient(
            GoogleNetHttpTransport.newTrustedTransport(),
            JacksonFactory.getDefaultInstance(),
            new HttpCredentialsAdapter(new DefaultCredentialProvider().getCredential()));

    Operation operation =
        artifactRegistryClient
            .importArtifacts(
                repoProject.get(),
                location.get(),
                repository.get(),
                String.format("gs://%s/%s", blobId.getBucket(), blobId.getName()))
            .execute();

    while (!Boolean.TRUE.equals(operation.getDone())) {
      Thread.sleep(100);
      operation =
          artifactRegistryClient
              .projects()
              .locations()
              .operations()
              .get(operation.getName())
              .execute();
    }

    System.out.printf("OPERATION: %s\n", operation);
    return operation;
  }

  private BlobId uploadDebToGcs(Storage storage) throws IOException {
    BlobId blobId = BlobId.of(uploadBucket.get(), archiveFile.get().getAsFile().getName());
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
    try (ByteChannel fileChannel = Files.newByteChannel(archiveFile.get().getAsFile().toPath());
        WritableByteChannel gcsChannel = storage.writer(blobInfo)) {
      ByteStreams.copy(fileChannel, gcsChannel);
    }
    return blobId;
  }
}
