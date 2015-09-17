from distutils.core import setup
from pkg_resources import require

setup(
    name='rescal-matcher',
    version='',
    packages=['tools', 'extrescal'],
    url='http://researchstudio-sat.github.io/webofneeds/',
    license='',
    author='',
    author_email='',
    description=''
)

require('numpy', 'scipy')