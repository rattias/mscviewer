from msc.gui import msc_fun 
from msc.utils import msc_print

@msc_fun
def hello_something(text):
  msc_print('Hello '+text)

@msc_fun
def hello_world():
    hello_something('World')

@msc_fun
def hello_world_or_not(text='World'):
    hello_something(text)

if __name__ == '__main__':
    hello_world()

