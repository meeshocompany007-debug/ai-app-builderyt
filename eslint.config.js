import js from "@eslint/js";
export default [
  js.configs.recommended,
  {
    languageOptions: {
      globals: Object.fromEntries(
        [
          "document",
          "fetch",
          "Option",
          "EventSource",
          "location",
          "FormData",
          "process",
          "Buffer",
          "URL",
          "setInterval",
          "clearInterval",
          "setTimeout",
          "clearTimeout",
          "console",
          "AbortSignal",
        ].map((x) => [x, "readonly"]),
      ),
    },
  },
];
