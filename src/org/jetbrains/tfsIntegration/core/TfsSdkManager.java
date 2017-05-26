package org.jetbrains.tfsIntegration.core;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.PluginPathManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ObjectUtils;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.jni.loader.NativeLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.configuration.Credentials;
import org.jetbrains.tfsIntegration.core.configuration.TFSConfigurationManager;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;

import java.io.File;
import java.nio.file.Path;

public class TfsSdkManager {

  private TfsSdkManager() {
    setupNativeLibrariesPath();
  }

  @NotNull
  public Path getCacheFile() {
    return DefaultPersistenceStoreProvider.INSTANCE.getCachePersistenceStore().getStoreFile().toPath().resolve("VersionControl.config");
  }

  @NotNull
  public com.microsoft.tfs.core.httpclient.Credentials getCredentials(@NotNull ServerInfo server) {
    Credentials credentials = ObjectUtils.assertNotNull(TFSConfigurationManager.getInstance().getCredentials(server.getUri()));
    com.microsoft.tfs.core.httpclient.Credentials result;

    switch (credentials.getType()) {
      case NtlmNative:
        result = new DefaultNTCredentials();
        break;
      case NtlmExplicit:
      case Alternate:
        result = new UsernamePasswordCredentials(credentials.getQualifiedUsername(), credentials.getPassword());
        break;
      default:
        throw new IllegalArgumentException("Unknown credentials type " + credentials.getType());
    }

    return result;
  }

  @NotNull
  public static TfsSdkManager getInstance() {
    return ServiceManager.getService(TfsSdkManager.class);
  }

  public static void activate() {
    getInstance();
  }

  private static void setupNativeLibrariesPath() {
    File nativeLibrariesPath = new File(getPluginDirectory(), FileUtil.toSystemDependentName("lib/native"));

    System.setProperty(NativeLoader.NATIVE_LIBRARY_BASE_DIRECTORY_PROPERTY, nativeLibrariesPath.getPath());
  }

  @NotNull
  private static File getPluginDirectory() {
    PluginId pluginId = PluginId.getId("TFS");
    IdeaPluginDescriptor pluginDescriptor = ObjectUtils.assertNotNull(PluginManager.getPlugin(pluginId));

    return pluginDescriptor.isBundled()
           ? PluginPathManager.getPluginHome("tfsIntegration")
           : ObjectUtils.assertNotNull(pluginDescriptor.getPath());
  }
}
