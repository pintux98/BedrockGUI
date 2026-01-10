import React from "react";

type Props = { children: React.ReactNode };
type State = { hasError: boolean; error?: Error };

export class ErrorBoundary extends React.Component<Props, State> {
  state: State = { hasError: false };
  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }
  render() {
    if (this.state.hasError) {
      return (
        <div className="p-4 text-red-400">
          <div className="font-semibold mb-2">Something went wrong</div>
          <div className="text-sm">{this.state.error?.message}</div>
        </div>
      );
    }
    return this.props.children;
  }
}

