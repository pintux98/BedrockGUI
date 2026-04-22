import React, { useState, useEffect } from "react";

interface BufferedInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  value: string | number;
  onCommit: (value: string) => void;
}

export function BufferedInput({ value, onCommit, onChange, onBlur, onKeyDown, ...props }: BufferedInputProps) {
  const [localValue, setLocalValue] = useState(String(value ?? ""));

  useEffect(() => {
    setLocalValue(String(value ?? ""));
  }, [value]);

  const handleCommit = () => {
    if (localValue !== String(value ?? "")) {
      onCommit(localValue);
    }
  };

  return (
    <input
      {...props}
      value={localValue}
      onChange={(e) => {
        setLocalValue(e.target.value);
        onChange?.(e);
      }}
      onBlur={(e) => {
        handleCommit();
        onBlur?.(e);
      }}
      onKeyDown={(e) => {
        if (e.key === "Enter") {
          handleCommit();
        }
        onKeyDown?.(e);
      }}
    />
  );
}

interface BufferedTextAreaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  value: string;
  onCommit: (value: string) => void;
}

export function BufferedTextArea({ value, onCommit, onChange, onBlur, onKeyDown, ...props }: BufferedTextAreaProps) {
  const [localValue, setLocalValue] = useState(String(value ?? ""));

  useEffect(() => {
    setLocalValue(String(value ?? ""));
  }, [value]);

  const handleCommit = () => {
    if (localValue !== String(value ?? "")) {
      onCommit(localValue);
    }
  };

  return (
    <textarea
      {...props}
      value={localValue}
      onChange={(e) => {
        setLocalValue(e.target.value);
        onChange?.(e);
      }}
      onBlur={(e) => {
        handleCommit();
        onBlur?.(e);
      }}
      onKeyDown={(e) => {
        if (e.key === "Enter" && !e.shiftKey) {
           // Optional: commit on Enter or allow newlines? 
           // Usually textareas need Shift+Enter for newlines if Enter commits.
           // For now, let's rely on Blur for commit, or explicit Ctrl+Enter.
           // But standard behavior for multi-line inputs is usually just Blur.
        }
        onKeyDown?.(e);
      }}
    />
  );
}
