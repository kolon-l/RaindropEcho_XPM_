/* eslint-disable @typescript-eslint/naming-convention */

declare module "@blu3r4y/lzma" {
  type Mode = 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9;

  export function compress(data: string | Uint8Array, mode: Mode);
  export function compress(
    data: string | Uint8Array,
    mode: Mode,
    on_finish: (result: Uint8Array, error: any) => void,
    on_progress?: (percent: number) => void
  );

  export function decompress(data: Uint8Array);
  export function decompress(
    data: Uint8Array,
    on_finish: (result: string | Uint8Array, error: any) => void,
    on_progress?: (percent: number) => void
  );
}
