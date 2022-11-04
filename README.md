# react-native-geckoview

A fully functional implementation of `GeckoView` on android for react native. The component supports two-way messaging similar to react-native-webview.

## Basic Example

```ts
import GeckoView from "react-native-geckoview";
const App = () => {
  const ref = useRef<GeckoView>(null);

  return (
    <SafeAreaView
      style={{
        backgroundColor: "white",
        flex: 1,
      }}
    >
      <GeckoView
        ref={ref}
        style={{
          flex: 1,
        }}
        source={{ uri: "https://google.com" }}
        onLoadingStart={(e) => {
          console.log(e.nativeEvent.uri);
        }}
        onLoadingFinish={(e) => {
          console.log("success:", e.nativeEvent.success);
        }}
        remoteDebugging={true}
        onMessage={(event) => {
          console.log(event.nativeEvent);
        }}
        injectedJavaScript={
          'window.ReactNativeWebView.postMessage("hello world")'
        }
      />
    </SafeAreaView>
  );
};
```

## API

`GeckoView` implements a simple interface with basic url loading & messaging features.

```ts
interface GeckoViewInterface extends ViewProps {
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
```

## Commands

A small set of commands can also be invoked on `ref` of `GeckoView` for basic messaging & page navigation.

```ts
type WebViewCommands = {
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
```

Commands can be invoked on the `GeckoView` ref.

```ts
const ref = useRef<GeckoView>(null);

// In your function
ref.current?.reload();
```

## Messaging

Messaging works through an extension for `GeckoView` which can be found at the root of the project. Copy contents of `extensions/messaging` to your Apps's `android/app/src/main/assets/messaging` folder.

## Thanks to
- [fushixiang](https://github.com/a251115100/geckoview-jsdemo) for GeckoView extension example.
- [sunnylqm](https://github.com/sunnylqm/react-native-geckoview) for basic example of GeckoView.

## MIT License
