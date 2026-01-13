# LZMA-JS ES6 + Types

This is an E6 module with type definitions of the [LZMA-JS](https://github.com/LZMA-JS/LZMA-JS) package by [Nathan Rugg](https://github.com/nmrugg), which was then extended by [Kaito Udagawa](https://github.com/umireon).

LZMA-JS is a JavaScript implementation of the Lempel-Ziv-Markov (LZMA) chain compression algorithm.
Please find full details in the original README: https://github.com/LZMA-JS/LZMA-JS

## Installation

Install with NPM or Yarn.

```sh
npm install @blu3r4y/lzma
```

```sh
yarn add @blu3r4y/lzma
```

## Usage

Compress and decompress a string with compression level 1.

```js
import { compress, decompress } from "@blu3r4y/lzma";

const data = "Hello World!";
const compressed = compress(data, 1);
const decompressed = decompress(result);

// data === decompressed
```

You may also use the callback interface or supply byte arrays.  
The interface is the same as in the original package.

```ts
function compress(data: string | Uint8Array, mode: Mode);
function compress(
    data: string | Uint8Array,
    mode: Mode,
    on_finish: (result: Uint8Array, error: any) => void,
    on_progress?: (percent: number) => void
);

function decompress(data: Uint8Array);
function decompress(
    data: Uint8Array,
    on_finish: (result: string | Uint8Array, error: any) => void,
    on_progress?: (percent: number) => void
);
```
