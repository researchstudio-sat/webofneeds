/**
 * Created by ms on 23.06.2020
 */

export function readRdfStream(stream) {
  return new Promise((resolve, reject) => {
    let data = "";
    stream.on("data", chunk => (data += chunk));
    stream.on("end", () => resolve(data));
    stream.on("error", error => reject(error));
  });
}
