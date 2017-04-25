from scipy.io.mmio import mmwrite

__author__ = 'hfriedrich'

import logging
import codecs
import sys
import numpy as np
from scipy.io import mmread
from scipy.sparse import csr_matrix, lil_matrix
from scipy.spatial.distance import pdist
from scipy.spatial.distance import squareform
from extrescal.extrescal import rescal

logging.basicConfig(level=logging.INFO, stream=sys.stdout,
                    format='%(asctime)s %(levelname)-8s %(message)s',
                    datefmt='%a, %d %b %Y %H:%M:%S')
_log = logging.getLogger()

# This file contains util functions for the processing of the tensor (including handling
# of needs, attributes, etc.)

class SparseTensor:

        CONNECTION_SLICE = 0

        def __init__(self, headers):
            self.shape = (len(headers), len(headers))
            self.data = []
            self.headers = list(headers)

        def copy(self):
            copyTensor = SparseTensor(self.headers)
            for i in range(len(self.data)):
                copyTensor.addSliceMatrix(self.data[i], i)
            return copyTensor

        def getMatrixShape(self):
            return self.shape

        def getSliceMatrix(self, slice):
            return self.data[slice].copy()

        def getSliceMatrixList(self):
            list = [slice.copy() for slice in self.data]
            return list

        def addSliceMatrix(self, matrix, slice):
            if self.shape != matrix.shape:
                raise Exception("Bad shape of added slices of tensor, is (%d,%d) but should be (%d,%d)!" %
                                (matrix.shape[0], matrix.shape[1], self.shape[0], self.shape[1]))
            self.data.append(csr_matrix(matrix))

        def getHeaders(self):
            return list(self.headers)

        def getArrayFromSliceMatrix(self, slice, indices):
            return matrix_to_array(self.data[slice], indices)

        # return a list of indices which refer to rows/columns of needs in the tensor
        def getNeedIndices(self):
            needs = [i for i in range(0, len(self.getHeaders())) if (self.getHeaders()[i].startswith('Need:'))]
            return needs

        # return a list of indices which refer to rows/columns of attributes in the tensor
        def getAttributeIndices(self):
            attrs = [i for i in range(0, len(self.getHeaders())) if (self.getHeaders()[i].startswith('Attr:'))]
            return attrs

        def getNeedLabel(self, need):
            return self.getHeaders()[need][6:]

        def getAttributesForNeed(self, need, slice):
            attr = self.data[slice][need,].nonzero()[1]
            attr = [self.getHeaders()[i][6:] for i in attr]
            return attr

        def hasConnection(self, need1, need2):
            return (self.getSliceMatrix(SparseTensor.CONNECTION_SLICE)[need1,need2] != 0)

        # return the "need x need" matrix and their connections between them without attributes for the extension of
        # the rescal algorithm extrescal
        def getPureNeedConnectionMatrix(self):
            return self.getSliceMatrix(SparseTensor.CONNECTION_SLICE)

        # return the "need x attribute" matrix D for the extension of the rescal algorithm extrescal
        def getNeedAttributeMatrix(self):
            D = self.getSliceMatrix(1)
            for i in range(2, len(self.data)):
                D = D + self.getSliceMatrix(i)
            attrIndices = self.getAttributeIndices()
            D = D.tocsc()[:, attrIndices]
            return D.tocsr()



# read the input tensor data (e.g. data-0.mtx ... ) and the headers file (e.g. headers.txt)
# if adjustDim is True then the dimensions of the slice matrix
# files are automatically adjusted to fit to biggest dimensions of all slices
def read_input_tensor(headers_filename, data_file_names, adjustDim=False):

    #load the header file
    _log.info("Read header input file: " + headers_filename)
    input = codecs.open(headers_filename,'r',encoding='utf8')
    headers = input.read().splitlines()
    input.close()

    # get the largest dimension of all slices
    if adjustDim:
        maxDim = 0
        for data_file in data_file_names:
            matrix = mmread(data_file)
            if maxDim < matrix.shape[0]:
                maxDim = matrix.shape[0]
            if maxDim < matrix.shape[1]:
                maxDim = matrix.shape[1]

    # load the data files
    slice = 1
    tensor = SparseTensor(headers)
    for data_file in data_file_names:
        if adjustDim:
            adjusted = adjust_mm_dimension(data_file, maxDim)
            if adjusted:
                _log.warn("Adujst dimension to (%d,%d) of matrix file: %s" % (maxDim, maxDim, data_file))

        if data_file.endswith("connection.mtx"):
            _log.info("Read as slice %d the data input file: %s" % (0, data_file))
            matrix = mmread(data_file)
            tensor.addSliceMatrix(matrix, 0)
        else:
            _log.info("Read as slice %d the data input file: %s" % (slice, data_file))
            matrix = mmread(data_file)
            tensor.addSliceMatrix(matrix, slice)
            slice = slice + 1
    return tensor

# adjust (increase) the dimension of an mm matrix file
def adjust_mm_dimension(data_file, dim):
    file = codecs.open(data_file,'r',encoding='utf8')
    lines = file.read().splitlines()
    file.close()
    for line in lines:
        if not line.startswith('%'):
            vals = line.split(' ')
            if (int(vals[0]) == dim and int(vals[1]) == dim):
                return False

    file = codecs.open(data_file,'w+',encoding='utf8')
    found = False
    for line in lines:
        if not line.startswith('%') and not found:
            vals = line.split(' ')
            newLine = str(dim) + " " + str(dim) + " " + vals[2]
            file.write(newLine + "\n")
            found = True
        else:
            file.write(line + "\n")
    file.close()
    return True

def execute_extrescal(input_tensor, rank, init='nvecs', conv=1e-4, lmbda=0):

    temp_tensor = [input_tensor.getPureNeedConnectionMatrix()]
    D = input_tensor.getNeedAttributeMatrix()

    _log.info('start extrescal processing ...')
    _log.info('config: init=%s, conv=%f, lmbda=%f' % (init, conv, lmbda))
    _log.info('Tensor: %d x %d x %d | Attribute Matrix: %d x %d | Rank: %d' % (
        temp_tensor[0].shape + (len(temp_tensor),) + D.shape + (rank,))
    )

    result  = rescal(temp_tensor, D, rank, init=init, conv=conv, lmbda=lmbda)
    _log.info('extrescal stopped processing')
    A = result[0]
    R = result[1]
    return A, R

# create a similarity matrix of needs (and attributes)
def similarity_ranking(A):
    dist = squareform(pdist(A, metric='cosine'))
    return dist

# return the specified indices from a sparse matrix as an numpy array
def matrix_to_array(m, indices):
    return np.array(m[indices])[0]


# for rescal algorithm output predict hints
# Parameters:
# - A, R: result matrices of rescal algorithm
# - threshold: write out only those predictions that are above the threshold
# - mask_matrix: matrix with binary entries, 1 specifies where predictions should be calculated
# - keepScore: if true keep the original score of the predictions, otherwise set all above the threshold to 1
def predict_rescal_hints_by_threshold(A, R, threshold, mask_matrix, keepScore=True):

    # compute prediction array with scores
    hint_prediction_matrix = np.dot(A, np.dot(R[SparseTensor.CONNECTION_SLICE], A.T))

    # choose indices above threshold to keep
    hint_indices = hint_prediction_matrix > threshold
    if not keepScore:
        hint_prediction_matrix[hint_indices] = 1
    hint_mask_matrix = np.zeros(hint_prediction_matrix.shape)
    hint_mask_matrix[hint_indices] = 1

    # return the calculated predictions
    hint_mask_matrix = np.multiply(mask_matrix, hint_mask_matrix)
    hint_prediction_matrix = np.multiply(hint_mask_matrix, hint_prediction_matrix)
    return csr_matrix(hint_prediction_matrix)

# create a binary mask matrix for hint prediction, 1 specifies where predictions should be calculated.
# the mask contains by default entries between needs of need types that match each other and removes
# entries for connections of the tensor that were already available
# Parameters:
# - tensor: tensor for which the predictions are computed
# - symmetric: create a symmetric mask
# - keepConnections: if true keep the predictions between the needs where a connection existed before
def create_hint_mask_matrix(tensor, symmetric=False, keepConnections=False):

    # use only need to need indices for hint connection prediction
    need_indices = np.zeros(tensor.getMatrixShape()[0])
    need_indices[tensor.getNeedIndices()] = 1
    need_vector = need_indices[np.newaxis]
    need_matrix = need_vector * need_vector.T
    np.fill_diagonal(need_matrix, 0)
    mask_matrix = need_matrix

    # optionally exclude already existing connections from prediction
    if not keepConnections:
        connection_array = np.asarray(tensor.getSliceMatrix(SparseTensor.CONNECTION_SLICE).toarray())
        connection_indices = connection_array > 0.0
        mask_matrix[connection_indices] = 0

    # symmetric mask needed?
    if not symmetric:
        mask_matrix = np.tril(mask_matrix)

    return mask_matrix