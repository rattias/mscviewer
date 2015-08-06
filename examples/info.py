from msc.model import *

model = events()
for ev in model:
    en = event_entity(ev)
    en_name = entity_path(en)
    label = event_label(ev)
    print 'entity=', en_name, 'label=', label


