import pdfMake from "pdfmake/build/pdfmake.min";
import pdfFonts from "pdfmake/build/vfs_fonts";

export function exportPdf(docDefinition) {
  if (!docDefinition) return;
  pdfMake.vfs = pdfFonts.pdfMake.vfs;
  pdfMake.createPdf(docDefinition).download();
}
