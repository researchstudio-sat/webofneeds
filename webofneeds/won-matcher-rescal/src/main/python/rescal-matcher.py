import argparse
import logging
import sys
import os
from scipy.io import mmwrite

__author__ = 'hfriedrich'

logging.basicConfig(level=logging.INFO, stream=sys.stdout,
                    format='%(asctime)s %(levelname)-8s %(message)s',
                    datefmt='%a, %d %b %Y %H:%M:%S')
_log = logging.getLogger()


from tools.tensor_utils import read_input_tensor, SparseTensor, execute_extrescal, \
    predict_rescal_hints_by_threshold, test_create_hint_mask_matrix, test_predict_rescal_hints_by_threshold


# ==========================================================================
# Script that executes the rescal algorithm on the input data in a folder
# and writes the predicted connection output matrix based on a threshold
# to the file "hints.mx" in the same folder
# ===========================================================================
if __name__ == '__main__':

    # cmd line parsing
    parser = argparse.ArgumentParser(description='link prediction algorithm evaluation script')
    parser.add_argument('-inputfolder',
                        action="store", dest="inputfolder", required=True,
                        help="folder with tensor input files")
    parser.add_argument('-outputfolder',
                        action="store", dest="outputfolder", required=True,
                        help="folder for tensor output files")
    parser.add_argument('-rank',
                        action="store", dest="rank", default="500", type=int,
                        help="rank of rescal algorithm")
    parser.add_argument('-threshold',
                        action="store", dest="threshold", type=float, required=True,
                        help="threshold of rescal algorithm to produce hints")
    args = parser.parse_args()

    # load the tensor
    header_file = "headers.txt"
    atom_indices_file = "atomIndices.txt"

    slice_files = []
    for file in os.listdir(args.inputfolder):
        if file.endswith(".mtx"):
            slice_files.append(file)

    header_input = args.inputfolder + "/" + header_file
    atom_indices_input = args.inputfolder + "/" + atom_indices_file
    data_input = []
    for slice in slice_files:
        data_input.append(args.inputfolder + "/" + slice)
    input_tensor = read_input_tensor(header_input, atom_indices_input, data_input, True)

    # execute rescal
    A,R = execute_extrescal(input_tensor, args.rank)

    # predict new hints
    _log.info("predict hints with threshold: %f" % args.threshold)
    connection_prediction = predict_rescal_hints_by_threshold(A, R, args.threshold, input_tensor)

    _log.info("number of hints created: %d" % len(connection_prediction.nonzero()[0]))

    # write the hint output matrix
    output  = args.outputfolder + "/" + "hints.mtx"
    _log.info("write hint prediction output matrix: " + output)
    mmwrite(output, connection_prediction)