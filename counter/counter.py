from pprint import pprint
from collections import defaultdict
import glob

def create_record():
    return {
        'rule': None,
        'totals': {
            'extended': 0,
            'centralized': 0,
            'cac_prolog': 0,
            'cac_kotlin': 0
        },
        'subrules': []
    }

def parse(filename):
    with open(filename) as f:
        lines = f.readlines()

    scr = create_record()
    final = []

    status = 'extended'
    started = False

    for l in lines:
        # --- DEBUG ---
        if l.startswith(':- deleteResource(resources_uwpaxxhshi).'):
            started = True
        # -------------

        if not started:
            if l.startswith('# -- start_counter --'):
                started = True
            continue
        # print("Parsing: {}".format(l))

        # new operation
        if l.startswith(":- "):
            # extended time includes centralized and cac_prolog
            scr['totals']['extended'] -= (
                scr['totals']['centralized'] +
                scr['totals']['cac_prolog']
            )

            final.append(scr)
            scr = create_record()
            scr['rule'] = l.split(":- ")[1].split("(")[0]
            status = 'extended'
        
        # a CAC operation is invoked
        elif l.startswith("[CryptoAC]"):
            scr['subrules'].append(l.split(' ')[1])
            status = 'cac_kotlin'

        # a CryptoAC operation has been executed
        elif l.startswith("% Employed "):
            if status == 'cac_kotlin':
                scr['totals']['cac_kotlin'] += int(l.split("% Employed time: ")[1]) * 10
                status = 'cac_prolog'
            else: 
                print("Problema")
                exit(100)

        # a centralized operation is invoked
        elif l.startswith("[TRADIT]"):
            status = 'centralized'

        # a sub computation is invoked
        elif l.startswith("%"):
            time = int(float(l.split(" CPU in ")[1].split(" seconds ")[0]) * 1000)
            if time <= 0:
                inf = int(l.split('% ')[1].split(' inferences, ')[0].replace(',', ''))
                teo = int(l.split(' CPU, ')[1].split(' Lips)')[0])
                time = int((inf / teo) * 10_000)
            scr['totals'][status] += time
            status = 'extended'

        # a tuples has been modified
        elif l.startswith("[CRYPTO]"):
            pass # just ignore them

        # not recognized
        else: 
            # print("Not recognized")
            pass

    print("--- {} ---".format(filename))

    counter = defaultdict(int)
    subcounter = defaultdict(int)
    for i in final:
        counter[i['rule']] += 1
        for sr in i['subrules']:
            subcounter[sr] += 1

    tot_extended = sum([i['totals']['extended'] for i in final])
    tot_centralized = sum([i['totals']['centralized'] for i in final])
    tot_cacprolog = sum([i['totals']['cac_prolog'] for i in final])
    tot_kotlin = sum([i['totals']['cac_kotlin'] for i in final])

    print('tot_extended={}'.format(tot_extended))
    print('tot_centralized={}'.format(tot_centralized))
    print('tot_cacprolog={}'.format(tot_cacprolog))
    print('tot_kotlin={}'.format(tot_kotlin))
    print('tot={}'.format(sum([tot_extended, tot_centralized, tot_cacprolog, tot_kotlin])))
    pprint(counter)
    pprint(subcounter)

    # pprint(final)

def main():
    for fn in glob.glob("*.final.txt"):
        parse(fn)

if __name__ == '__main__':
    main()