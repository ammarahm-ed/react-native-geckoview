import React, { createRef, ReactNode, RefObject } from "react";
import {
  findNodeHandle,
  NativeSyntheticEvent,
  requireNativeComponent,
  UIManager,
  ViewProps,
} from "react-native";

export interface GeckoViewInterface extends ViewProps {
  source?: { html?: string; uri?: string };
  forceDarkOn?: boolean;
  autoFillEnabled?: boolean;
  remoteDebugging?: boolean;
  userAgent?: string;
  onLoadingStart?: (event: NativeSyntheticEvent<{ uri: string }>) => void;
  onLoadingFinish?: (event: NativeSyntheticEvent<{ success: string }>) => void;
  onLoadingError?: (
    event: NativeSyntheticEvent<{ error: string; uri: string }>
  ) => void;
  onMessage?: (event: NativeSyntheticEvent<any>) => void;
  onLoadingProgress?: (
    event: NativeSyntheticEvent<{ progress: number }>
  ) => void;
  onMessagingDisconnected?: () => void;
  injectedJavaScript?: string;
}

export type WebViewCommands = {
  goBack: () => void;
  goForward: () => void;
  reload: () => void;
  stopLoading: () => void;
  postMessage: (message: string) => void;
  injectJavaScript: (script: string) => void;
  loadUrl: (url: string) => void;
  requestFocus: () => void;
  clearHistory: () => void;
  clearCache: () => void;
  connectMessagingPort: () => void;
};

export default class GeckoView extends React.Component<GeckoViewInterface> {
  currentViewRef: RefObject<typeof GeckoViewNative>;
  constructor(props: GeckoViewInterface) {
    super(props);
    this.currentViewRef = createRef();
  }

  dispatchCommand(command: keyof WebViewCommands, args?: any[]) {
    const node = findNodeHandle(this.currentViewRef.current);
    UIManager.dispatchViewManagerCommand(node, command, args || []);
  }

  goBack() {
    this.dispatchCommand("goBack");
  }
  goForward() {
    this.dispatchCommand("goForward");
  }
  reload() {
    this.dispatchCommand("reload");
  }
  stopLoading() {
    this.dispatchCommand("stopLoading");
  }
  postMessage(message: string) {
    this.dispatchCommand("postMessage", [message]);
  }
  injectJavaScript(script: string) {
    this.dispatchCommand("injectJavaScript", [script]);
  }
  loadUrl(url: string) {
    this.dispatchCommand("loadUrl", [url]);
  }
  requestFocus() {
    this.dispatchCommand("requestFocus");
  }
  clearHistory() {
    this.dispatchCommand("clearHistory");
  }
  clearCache() {
    this.dispatchCommand("clearCache");
  }
  connectMessaging() {
    this.dispatchCommand("connectMessagingPort");
  }

  render(): ReactNode {
    return (
      <GeckoViewNative
        style={this.props.style}
        ref={this.currentViewRef as any}
        forceDarkOn={this.props.forceDarkOn}
        autoFillEnabled={this.props.autoFillEnabled}
        source={this.props.source}
        onLoadingStart={this.props.onLoadingStart}
        onLoadingError={this.props.onLoadingError}
        onLoadingProgress={this.props.onLoadingProgress}
        onMessage={this.props.onMessage}
        onLoadingFinish={this.props.onLoadingFinish}
        onMessagingDisconnected={this.props.onMessagingDisconnected}
        injectedJavaScript={this.props.injectedJavaScript}
      />
    );
  }
}

const GeckoViewNative = requireNativeComponent<GeckoViewInterface>("GeckoView");
