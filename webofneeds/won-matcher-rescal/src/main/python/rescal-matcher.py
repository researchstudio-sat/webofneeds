import argparse
import logging
from scipy.io import mmwrite

__author__ = 'hfriedrich'

logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s %(levelname)-8s %(message)s',
                    datefmt='%a, %d %b %Y %H:%M:%S')
_log = logging.getLogger()

from tools.tensor_utils import read_input_tensor, SparseTensor, execute_extrescal, \
    predict_rescal_hints_by_threshold


# ==========================================================================
# Script that executes the rescal algorithm on the input data in a folder
# and writes the predicted connection output matrix based on a threshold
# to the file "hints.mx" in the same folder
# ===========================================================================
if __name__ == '__main__':

    # cmd line parsing
    parser = argparse.ArgumentParser(description='link prediction algorithm evaluation script')
    parser.add_argument('-folder',
                        action="store", dest="folder", required=True,
                        help="folder with tensor input and output files")
    parser.add_argument('-rank',
                        action="store", dest="rank", default="500", type=int,
                        help="rank of rescal algorithm")
    parser.add_argument('-threshold',
                        action="store", dest="threshold", type=float, required=True,
                        help="threshold of rescal algorithm to produce hints")
    args = parser.parse_args()

    # load the tensor
    header_file = "headers.txt"
    slice_files = ["connection.mtx", "needtype.mtx", "subject.mtx", "content.mtx", "tag.mtx"]
    slice_types = SparseTensor.defaultSlices + [SparseTensor.ATTR_CONTENT_SLICE, SparseTensor.CATEGORY_SLICE]
    header_input = args.folder + "/" + header_file
    data_input = []
    for slice in slice_files:
        data_input.append(args.folder + "/" + slice)
    input_tensor = read_input_tensor(header_input, data_input, slice_types, True)

    # execute rescal
    A,R = execute_extrescal(input_tensor, args.rank)

    # predict hints (without the already known connections)
    _log.info("predict hints from connections with threshold: %f" % args.threshold)
    connection_prediction = predict_rescal_hints_by_threshold(A, R, args.threshold, input_tensor)
    _log.info("number of hints created: %d" % len(connection_prediction.nonzero()[0]))

    # write the hint output matrix
    output  = args.folder + "/" + "hints.mtx"
    _log.info("write connection prediction output matrix: " + output)
    mmwrite(output, connection_prediction)