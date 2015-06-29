from scipy.io.mmio import mmwrite

__author__ = 'hfriedrich'

import logging
import codecs
import numpy as np
from scipy.io import mmread
from scipy.sparse import csr_matrix, lil_matrix
from scipy.spatial.distance import pdist
from scipy.spatial.distance import squareform
from rescal import rescal_als
from extrescal.extrescal import rescal

logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s %(levelname)-8s %(message)s',
                    datefmt='%a, %d %b %Y %H:%M:%S')
_log = logging.getLogger()

# This file contains util functions for the processing of the tensor (including handling
# of needs, attributes, etc.)

class SparseTensor:

        CONNECTION_SLICE, NEED_TYPE_SLICE, ATTR_SUBJECT_SLICE, ATTR_CONTENT_SLICE, CATEGORY_SLICE = range(5)
        defaultSlices = [CONNECTION_SLICE, NEED_TYPE_SLICE, ATTR_SUBJECT_SLICE]

        def __init__(self, headers, offerString="Attr: OFFER", wantString="Attr: WANT"):
            self.shape = (len(headers), len(headers))
            self.data = [csr_matrix(np.zeros(shape=self.shape))] * 5
            self.headers = list(headers)
            self.offerString = offerString
            self.wantString = wantString

        def copy(self):
            copyTensor = SparseTensor(self.headers, self.offerString, self.wantString)
            for i in range(len(self.data)):
                copyTensor.addSliceMatrix(self.data[i], i)
            return copyTensor

        def getShape(self):
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
            self.data[slice] = csr_matrix(matrix)

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

        # return a list of indices which refer to rows/columns of needs of type OFFER in the tensor
        def getOfferIndices(self):
            needs = self.getNeedIndices()
            offer_attr_idx = self.getHeaders().index(self.offerString)
            offers = [need for need in needs if
                      (self.getSliceMatrix(SparseTensor.NEED_TYPE_SLICE)[need, offer_attr_idx] == 1)]
            return offers

        # return a list of indices which refer to rows/columns of needs of type WANT in the tensor
        def getWantIndices(self):
            needs = self.getNeedIndices()
            want_attr_idx = self.getHeaders().index(self.wantString)
            wants = [need for need in needs if
                     (self.getSliceMatrix(SparseTensor.NEED_TYPE_SLICE)[need, want_attr_idx] == 1)]
            return wants

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
            # conSlice = self.getSliceMatrix(SparseTensor.CONNECTION_SLICE)
            # indices = self.getNeedIndices()
            # conSlice = conSlice.tocsc()[:,indices]
            # return conSlice.tocsr()[indices,:]
            return self.getSliceMatrix(SparseTensor.CONNECTION_SLICE)

        # return the "need x attribute" matrix D for the extension of the rescal algorithm extrescal
        def getNeedAttributeMatrix(self):
            D = self.getSliceMatrix(1)
            for i in range(2, len(self.data)):
                D = D + self.getSliceMatrix(i)
            # needIndices = self.getNeedIndices()
            attrIndices = self.getAttributeIndices()
            D = D.tocsc()[:, attrIndices]
            # return D.tocsr()[needIndices, :]
            return D.tocsr()



# read the input tensor data (e.g. data-0.mtx ... data-3.mtx) and
# the headers file (e.g. headers.txt)
# if adjustDim is True then the dimensions of the slice matrix
# files are automatically adjusted to fit to biggest dimensions of all slices
def read_input_tensor(headers_filename, data_file_names, tensor_slices, adjustDim=False, offerString="Attr: OFFER",
                      wantString="Attr: WANT"):

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
    slice = 0
    tensor = SparseTensor(headers, offerString, wantString)
    for data_file in data_file_names:
        if adjustDim:
            adjusted = adjust_mm_dimension(data_file, maxDim)
            if adjusted:
                _log.warn("Adujst dimension to (%d,%d) of matrix file: %s" % (maxDim, maxDim, data_file))
        _log.info("Read as slice %d the data input file: %s" % (slice, data_file))
        matrix = mmread(data_file)
        tensor.addSliceMatrix(matrix, tensor_slices[slice])
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

# return a tuple with two lists holding need indices that represent connections
# between these needs, symmetric connection are only represented once
def connection_indices(tensor):
    nz = tensor.getSliceMatrix(SparseTensor.CONNECTION_SLICE).nonzero()
    nz0 = [nz[0][i] for i in range(len(nz[0])) if nz[0][i] <= nz[1][i]]
    nz1 = [nz[1][i] for i in range(len(nz[0])) if nz[0][i] <= nz[1][i]]
    indices = [i for i in range(len(nz0))]
    np.random.shuffle(indices)
    ret0 = [nz0[i] for i in indices]
    ret1 = [nz1[i] for i in indices]
    nzsym = (ret0, ret1)
    return nzsym

# execute the recal algorithm
def execute_rescal(input_tensor, rank, useNeedTypeSlice=True, useConnectionSlice=True, init='nvecs', conv=1e-4,
                   lambda_A=0, lambda_R=0, lambda_V=0):

    temp_tensor = input_tensor.getSliceMatrixList()
    if not (useNeedTypeSlice):
        _log.info('Do not use needtype slice for RESCAL')
        del temp_tensor[SparseTensor.NEED_TYPE_SLICE]
    if not (useConnectionSlice):
        _log.info('Do not use connection slice for RESCAL')
        del temp_tensor[SparseTensor.CONNECTION_SLICE]

    _log.info('start rescal processing ...')
    _log.info('config: init=%s, conv=%f, lambda_A=%f, lambda_R=%f, lambda_V=%f' %
              (init, conv, lambda_A, lambda_R, lambda_V))
    _log.info('Tensor: %d x %d x %d | Rank: %d' % (
        temp_tensor[0].shape + (len(temp_tensor),) + (rank,))
    )

    A, R, _, _, _ = rescal_als(
        temp_tensor, rank, init=init, conv=conv,
        lambda_A=lambda_A, lambda_R=lambda_R, lambda_V=lambda_V, compute_fit='true'
    )

    _log.info('rescal stopped processing')
    return A, R


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

    # fill the Matrix A with 0's rows (for the attributes) to match the default rescal format (needs and attributes
    # intermixed in tensor, no extra attribute matrix)
    # needIndices = input_tensor.getNeedIndices()
    # if 0 in needIndices:
    #     A_default = A[0,:]
    # else:
    #     A_default = np.zeros((1, A.shape[1]))
    # j = 1
    # for i in range(1, len(input_tensor.getHeaders())):
    #     if i in needIndices:
    #         A_default = np.vstack((A_default, A[j,:]))
    #         j += 1
    #     else:
    #         A_default = np.vstack((A_default, np.zeros((1, A.shape[1]))))
    # return A_default, R
    return A, R

# execute the rescal algorithm and return a prediction tensor
def predict_rescal_als(input_tensor, rank, useNeedTypeSlice=True, useConnectionSlice=True):
    A,R = execute_rescal(input_tensor, rank, useNeedTypeSlice, useConnectionSlice)

    n = A.shape[0]
    P = np.zeros((n, n, len(R)))
    for k in range(len(R)):
        P[:, :, k] = np.dot(A, np.dot(R[k], A.T))

    return P, A, R

# create a similarity matrix of needs (and attributes)
def similarity_ranking(A):
    dist = squareform(pdist(A, metric='cosine'))
    return dist

# return the specified indices from a sparse matrix as an numpy array
def matrix_to_array(m, indices):
    return np.array(m[indices])[0]

# return the rescal predictions of the connection slice at the specified indices as an numpy array
# def predict_rescal_connections_array(A, R, indices):
#     result = [np.dot(A[indices[0][i],:], np.dot(R[SparseTensor.CONNECTION_SLICE], A[indices[1][i],:])) for i in range(len(indices[0]))]
#     return result

# return the rescal predictions of the connection slice at the specified indices as an numpy array
def predict_rescal_connections_array(A, R, indices):
    # result = [np.dot(A[indices[0][i],:], np.dot(R[SparseTensor.CONNECTION_SLICE], A[indices[1][i],:]))
    #           for i in range(len(indices[0]))]
    # due to performance reasons choose this implementation, not the above one
    sorted_idx = np.argsort(indices[0])
    from_needs = [indices[0][i] for i in sorted_idx]
    to_needs = [indices[1][i] for i in sorted_idx]
    result = np.zeros(len(sorted_idx))
    need = None
    need_vector = None
    for i in range(len(from_needs)):
        if (need != from_needs[i]):
            need = from_needs[i]
            need_vector = np.dot(R[SparseTensor.CONNECTION_SLICE], A[need,:])
        result[sorted_idx[i]] = np.dot(A[to_needs[i],:], need_vector)
    return result

# for rescal algorithm output predict connections by fixed threshold (higher threshold means higher precision)
def predict_rescal_connections_by_threshold(A, R, threshold, all_offers, all_wants, test_needs):
    binary_prediction = lil_matrix(np.zeros(shape=(A.shape[0],A.shape[0])))
    for need in test_needs:
        if need in all_offers:
            all_needs = all_wants
        elif need in all_wants:
            all_needs = all_offers
        else:
            continue
        inner_product = np.dot(R[SparseTensor.CONNECTION_SLICE], A[need,:])
        for x in all_needs:
            if (np.dot(A[x,:], inner_product)) >= threshold:
                binary_prediction[need,x] = 1
    return csr_matrix(binary_prediction)

# for rescal algorithm output predict hints
# Parameters:
# - A, R: result matrices of rescal algorithm
# - threshold: write out only those predictions that are above the threshold
# - keepConnections: if true keep the predictions between the needs where a connection existed before
# - keepScore: if true keep the original score of the predictions, otherwise set all above the threshold o 1
def predict_rescal_hints_by_threshold(A, R, threshold, tensor, keepConnections=False, keepScore=True):

    # compute prediction array with scores
    hint_prediction_array = np.dot(A, np.dot(R[SparseTensor.CONNECTION_SLICE], A.T))

    # choose indices above threshold to keep
    hint_indices = hint_prediction_array > threshold
    mask_array = np.zeros(hint_prediction_array.shape)
    mask_array[hint_indices] = 1
    if not keepScore:
        hint_prediction_array[hint_indices] = 1

    # use only need to need indices for hint connection prediction
    need_indices = np.zeros(tensor.getShape()[0])
    need_indices[tensor.getNeedIndices()] = 1
    need_vector = need_indices[np.newaxis]
    need_array = need_vector * need_vector.T
    np.fill_diagonal(need_array, 0)
    mask_array = np.multiply(mask_array, need_array)

    # optionally exclude already existing connections from prediction
    if not keepConnections:
        connection_array = np.asarray(tensor.getSliceMatrix(SparseTensor.CONNECTION_SLICE).toarray())
        connection_indices = connection_array > 0.0
        mask_array[connection_indices] = 0

    # return the calculated predictions
    hint_prediction_array = np.multiply(mask_array, hint_prediction_array)
    return csr_matrix(hint_prediction_array)


# for rescal algorithm output predict connections by fixed threshold for each of the test_needs based on the
# similarity of latent need clusters (higher threshold means higher recall)
def predict_rescal_connections_by_need_similarity(A, threshold, all_offers, all_wants, test_needs):
    S = similarity_ranking(A)
    binary_prediction = lil_matrix(np.zeros(shape=(A.shape[0],A.shape[0])))
    for need in test_needs:
        if need in all_offers:
            all_needs = all_wants
        elif need in all_wants:
            all_needs = all_offers
        else:
            continue

        for x in all_needs:
            if S[need,x] < threshold:
                binary_prediction[need, x] = 1
    return csr_matrix(binary_prediction)

# extend the connection slice with transitive connections to the next hop to connected not only OFFERS and WANTS but
# also needs of the same type
def extend_next_hop_transitive_connections(tensor):
    con = tensor.getSliceMatrix(SparseTensor.CONNECTION_SLICE)
    con = con + con * con
    con.data = np.array([1.] * len(con.data))
    newTensor = tensor.copy()
    newTensor.addSliceMatrix(con, SparseTensor.CONNECTION_SLICE)
    return newTensor
