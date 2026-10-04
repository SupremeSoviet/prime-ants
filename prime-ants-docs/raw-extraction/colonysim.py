"""Python re-implementation of the Formic Frontier per-second colony passes.

Mirrors (as literally as possible):
  ColonyEconomy.tick, ColonyLogistics.tick, CasteJobLoop.tick,
  ColonyStageProgression.tick, CasteBalancer.tick, NativeBlockRole.tick
  (ColonySavedState.tickEconomy) followed by ColonyBuilder.tick (tickWorld).
Excluded: recurring events, raids, diplomacy/caravans, ant entity deliveries,
player actions (no research is ever started, no contracts delivered).
"""
import math
import sys

RES = ['food', 'ore', 'chitin', 'resin', 'fungus', 'venom', 'knowledge']
# caste: (foodUpkeep, foodCost, oreCost, chitinCost)
CASTE = {
    'worker': (1, 6, 0, 0), 'scout': (1, 4, 1, 0), 'miner': (2, 8, 4, 0),
    'soldier': (3, 10, 2, 3), 'major': (6, 24, 8, 10), 'giant': (24, 180, 45, 55),
    'queen': (12, 0, 0, 0),
}
CASTE_ORDER = ['worker', 'scout', 'miner', 'soldier', 'major', 'giant', 'queen']
# building costs food, ore, chitin, resin, fungus, venom, knowledge
BCOST = {
    'queen_chamber': (0, 0, 0, 0, 0, 0, 0), 'food_store': (24, 0, 0, 0, 0, 0, 0),
    'nursery': (32, 0, 8, 0, 0, 0, 0), 'mine': (20, 16, 0, 0, 0, 0, 0),
    'chitin_farm': (24, 4, 12, 0, 0, 0, 0), 'barracks': (36, 18, 16, 0, 0, 0, 0),
    'market': (28, 10, 6, 0, 0, 0, 0), 'diplomacy_shrine': (40, 14, 24, 0, 0, 0, 0),
    'watch_post': (18, 12, 8, 0, 0, 0, 0), 'resin_depot': (32, 8, 8, 0, 0, 0, 0),
    'pheromone_archive': (48, 16, 20, 8, 4, 0, 0), 'fungus_garden': (28, 0, 8, 4, 0, 0, 0),
    'venom_press': (36, 18, 18, 12, 6, 0, 0), 'armory': (42, 28, 24, 16, 0, 4, 0),
    'great_mound': (120, 48, 96, 48, 24, 0, 36), 'queen_vault': (80, 36, 120, 42, 18, 0, 48),
    'trade_hub': (96, 30, 72, 54, 12, 0, 54), 'road': (4, 0, 0, 0, 0, 0, 0),
}
# culture: foodB, resinB, fungusB, venomB, workerBias, scoutBias, constructionBonus, starterQueue
CULT = {
    'amber': (0, 0, 0, 0, 0, 0, 0, ['diplomacy_shrine', 'market', 'pheromone_archive']),
    'leafcutter': (2, 0, 2, 0, 1, 1, 0, ['fungus_garden', 'chitin_farm', 'market', 'pheromone_archive']),
    'fire': (0, 0, 0, 2, 0, 0, 1, ['watch_post', 'armory', 'market', 'pheromone_archive']),
    'carpenter': (0, 1, 0, 0, 0, 0, 2, ['resin_depot', 'market', 'pheromone_archive']),
}
RANKS = [('outpost', 0, 18, 0), ('burrow', 35, 24, 2), ('hive', 85, 36, 5), ('citadel', 155, 48, 9)]
STAGES = [('founding', 0, 0, 0, None), ('growth', 3, 120, 40, 'barracks'),
          ('established', 6, 400, 180, 'pheromone_archive'), ('mature', 12, 900, 500, 'great_mound')]
STARTER_SEQUENCE = ['queen_chamber', 'food_store', 'nursery', 'mine', 'chitin_farm', 'barracks',
                    'market', 'pheromone_archive', 'armory', 'diplomacy_shrine']
UPGRADE_ORDER = ['market', 'resin_depot', 'pheromone_archive', 'food_store', 'nursery', 'mine',
                 'barracks', 'queen_chamber']
PLAYER_SUPPLY = ('construction ', 'research ', 'repair ', 'famine', 'migration', 'invasion', 'treaty', 'expansion')


class Colony:
    def __init__(self, culture, rep):
        self.culture = culture
        self.c = CULT[culture]
        self.res = {r: 0 for r in RES}
        self.castes = {k: 0 for k in CASTE_ORDER}
        self.priorities = ['food', 'ore', 'chitin', 'defense']
        self.queen = 140
        self.age = 0
        self.stage = 0
        self.buildings = []  # dict(type, level, progress, disabled)
        self.queue = []
        self.requests = []  # dict(building, resource, needed, fulfilled, reason)
        self.research = set()
        self.active = None
        self.rep = rep
        self.log = []
        # seedEconomy
        self.res.update(food=120, ore=20, chitin=24, resin=24,
                        fungus=28 if culture == 'leafcutter' else 12,
                        venom=16 if culture == 'fire' else 4, knowledge=8)
        self.castes.update(queen=1, worker=3 + self.c[4], scout=1 + self.c[5], miner=2, soldier=2, major=1)
        for t in ['queen_chamber', 'food_store', 'nursery', 'mine', 'barracks']:
            self.buildings.append(dict(type=t, level=1, progress=100, disabled=0))
        self.queue.extend(self.c[7])

    # helpers -------------------------------------------------------------
    def add(self, r, d):
        self.res[r] = max(0, self.res[r] + d)

    def completed(self, t):
        return sum(1 for b in self.buildings if b['type'] == t and b['progress'] >= 100)

    def has_completed(self, t):
        return self.completed(t) > 0

    def pop(self):
        return sum(self.castes.values())

    def upkeep(self):
        return sum(CASTE[k][0] * v for k, v in self.castes.items())

    def queen_alive(self):
        return self.queen > 0 and self.castes['queen'] > 0

    def score(self):
        done = sum(1 for b in self.buildings if b['progress'] >= 100)
        return done * 10 + self.pop() * 2 + max(0, self.rep) + self.queen // 12

    def rank(self):
        s = self.score()
        r = 0
        for i, (_, th, _, _) in enumerate(RANKS):
            if s >= th:
                r = i
        return r

    def can_grow(self, k):
        _, f, o, ch = CASTE[k]
        return self.res['food'] >= f and self.res['ore'] >= o and self.res['chitin'] >= ch

    def grow(self, k):
        _, f, o, ch = CASTE[k]
        self.add('food', -f)
        self.add('ore', -o)
        self.add('chitin', -ch)
        self.castes[k] += 1

    def first_incomplete(self):
        for b in self.buildings:
            if b['progress'] < 100:
                return b
        return None

    def event(self, msg):
        self.log.append((self.age // 20, msg))

    # ColonyEconomy.tick ----------------------------------------------------
    def economy(self):
        self.age += 20
        w, s, m, so = (self.castes[k] for k in ('worker', 'scout', 'miner', 'soldier'))
        cf, fs, mi, nu, ba, ma, sh, rd, ar, fg, vp, am, wp = (self.completed(t) for t in (
            'chitin_farm', 'food_store', 'mine', 'nursery', 'barracks', 'market', 'diplomacy_shrine',
            'resin_depot', 'pheromone_archive', 'fungus_garden', 'venom_press', 'armory', 'watch_post'))
        rb = RANKS[self.rank()][3]
        fB, rB, fuB, vB = self.c[0:4]
        food = 4 + w * 3 + s + fs * 2 + ma + fg * 2 + rb + fB
        ore = m * 2 + mi + rb // 2
        chit = max(0, self.res['food'] // 25) + w // 2 + cf * 5 + nu + sh + rb // 2
        if 'chitin_cultivation' in self.research:
            chit += max(1, cf * 2)
        resin = rd * 4 + w // 3 + rB
        fung = fg * 4 + nu + fuB
        if 'fungus_symbiosis' in self.research:
            food += max(1, fg * 3)
            fung += 2
        ven = vp * 3 + vB
        if 'venom_drills' in self.research:
            ven += max(1, so // 3 + am)
        know = ar if (ar > 0 and self.active is None) else 0
        if self.culture == 'amber':
            know += sh + min(ma, sh)
        elif self.culture == 'leafcutter':
            food += fg * 2
            fung += fg
        elif self.culture == 'fire':
            ven += am * 2 + wp
        elif self.culture == 'carpenter':
            resin += rd * 2 + min(rd, ar)
        up = self.upkeep()
        self.add('food', food - up)
        self.add('ore', ore)
        self.add('chitin', chit)
        self.add('resin', resin)
        self.add('fungus', fung)
        self.add('venom', ven)
        self.add('knowledge', know)
        if not self.queen_alive():
            return
        g = self.choose_growth(so, ba)
        if g:
            self.grow(g)

    def choose_growth(self, soldiers, barracks):
        for p in self.priorities:
            g = self.priority_growth(p, soldiers, barracks)
            if g:
                return g
        if self.castes['worker'] < 3 and self.can_grow('worker'):
            return 'worker'
        if self.castes['miner'] < 2 and self.can_grow('miner'):
            return 'miner'
        if soldiers < 2 and self.can_grow('soldier'):
            return 'soldier'
        if self.res['food'] > 250 and 'mandible_plating' in self.research and self.can_grow('giant'):
            return 'giant'
        if self.res['chitin'] > 30 and self.can_grow('major'):
            return 'major'
        if self.castes['scout'] < 1 and self.can_grow('scout'):
            return 'scout'
        return None

    def priority_growth(self, p, soldiers, barracks):
        if p == 'food':
            if self.castes['worker'] < 8 + self.c[4] and self.can_grow('worker'):
                return 'worker'
            if self.castes['scout'] < 3 + self.c[5] and self.can_grow('scout'):
                return 'scout'
            return None
        if p == 'ore':
            return 'miner' if self.castes['miner'] < 8 and self.can_grow('miner') else None
        if p == 'chitin':
            if self.castes['major'] < 2 and self.can_grow('major'):
                return 'major'
            if self.castes['worker'] < 6 and self.can_grow('worker'):
                return 'worker'
            return None
        if p == 'defense':
            if soldiers < 3 + barracks * 2 and self.can_grow('soldier'):
                return 'soldier'
            if self.castes['major'] < 4 and self.can_grow('major'):
                return 'major'
            if self.res['food'] > 250 and 'mandible_plating' in self.research and self.can_grow('giant'):
                return 'giant'
            return None

    # ColonyLogistics.tick --------------------------------------------------
    def logistics(self):
        thr = max(1, self.castes['worker']) + (3 if self.has_completed('resin_depot') else 0) + self.c[4]
        i = 0
        while i < len(self.requests):
            rq = self.requests[i]
            if rq['fulfilled'] >= rq['needed']:
                self.requests.pop(i)
                continue
            if rq['reason'].lower().startswith(PLAYER_SUPPLY):
                i += 1
                continue
            avail = self.res[rq['resource']]
            if avail <= 0:
                i += 1
                continue
            d = min(rq['needed'] - rq['fulfilled'], avail, thr)
            if d > 0:
                self.add(rq['resource'], -d)
                rq['fulfilled'] += d
                break
            i += 1
        # tickResearch (no research ever started here)
        if self.active is None and self.has_completed('pheromone_archive'):
            self.add('knowledge', 1 + self.completed('pheromone_archive'))

    # CasteJobLoop.tick -----------------------------------------------------
    def jobloop(self):
        w, m, s = self.castes['worker'], self.castes['miner'], self.castes['scout']
        self.add('food', w)
        self.add('ore', m * 2)
        self.add('chitin', s)
        if w > 0:
            site = self.first_incomplete()
            if site:
                site['progress'] = min(100, site['progress'] + min(w, 10))
                if site['progress'] >= 100:
                    self.event('Builder crew completed ' + site['type'])
        # patrol: no damage in this sim
        if w > 0 and self.queen_alive() and self.queen < 140:
            self.queen = min(140, self.queen + min(w, 5))

    # ColonyStageProgression.tick ---------------------------------------------
    def stageprog(self):
        if not self.queen_alive():
            return
        age = self.age // 20
        comp = self.res['ore'] + self.res['chitin'] + self.res['resin']
        earned = 0
        for i, (_, a, f, c, _) in enumerate(STAGES):
            if i == 0:
                continue
            if age >= a and self.res['food'] >= f and comp >= c:
                earned = i
        if earned <= self.stage:
            return
        for i in range(self.stage + 1, earned + 1):
            sig = STAGES[i][4]
            unlocked = False
            if sig and not self.has_completed(sig) and sig not in self.queue and not any(b['type'] == sig for b in self.buildings):
                self.queue.append(sig)
                unlocked = True
            self.event('stage -> %s (signature %s %s)' % (STAGES[i][0], sig, 'ENQUEUED' if unlocked else 'no-op'))
        self.stage = earned

    # CasteBalancer.tick ------------------------------------------------------
    def balancer(self):
        if not self.queen_alive():
            return
        remaining = 2
        for p in self.priorities:
            if remaining <= 0:
                break
            need = None
            if p == 'food' and self.res['food'] < self.upkeep() * 2:
                need = 'worker'
            elif p == 'ore' and self.res['ore'] < 20:
                need = 'miner'
            elif p == 'chitin' and self.res['chitin'] < 20:
                need = 'worker'
            elif p == 'defense' and self.castes['soldier'] + self.castes['major'] < 2:
                need = 'soldier'
            if need is None:
                continue
            if need == 'worker':
                src = 'miner' if self.castes['miner'] > 2 else None
            else:
                src = 'worker' if self.castes['worker'] > 2 else None
            if src is None:
                continue
            avail = min(remaining, max(0, self.castes[src] - 2))
            if avail <= 0:
                continue
            self.castes[src] -= avail
            self.castes[need] += avail
            remaining -= avail
            self.event('rebalance %d %s -> %s' % (avail, src, need))

    # NativeBlockRole.tick ----------------------------------------------------
    def native(self):
        g = self.completed('fungus_garden')
        if g <= 0:
            return
        above = max(0, self.res['food'] - 40)
        run = min(g, above // 5)
        if run > 0:
            self.add('food', -run * 5)
            self.add('fungus', run * 8)

    # ColonyBuilder.tick ------------------------------------------------------
    def eff_cost(self, t, r):
        cost = BCOST[t][RES.index(r)]
        if cost > 0 and 'resin_masonry' in self.research:
            cost = max(1, math.ceil(cost * 0.9))
        return cost

    def builder(self):
        # claim radius ignored
        endgame = self.next_endgame()
        if endgame is None and self.maybe_upgrade():
            return
        self.enqueue_next(endgame)
        active = self.first_incomplete()
        if active is None:
            if self.start_queued():
                return
            active = self.first_incomplete()
        if active is None:
            return
        b = max(1, self.castes['worker'])
        active['progress'] = min(100, active['progress'] + 8 + b * 4 + self.c[6])
        if active['progress'] >= 100:
            self.event('Completed %s (lvl %d)' % (active['type'], active['level']))

    def maybe_upgrade(self):
        if self.queue or self.first_incomplete():
            return False
        cand = None
        for t in UPGRADE_ORDER:
            for b in self.buildings:
                if b['type'] == t and b['progress'] >= 100 and b['disabled'] == 0 and b['level'] < 2:
                    cand = b
                    break
            if cand:
                break
        if cand is None:
            return False
        costs = {}
        for r in RES:
            c = math.ceil(self.eff_cost(cand['type'], r) * 0.6)
            if r == 'resin':
                c += 10 + cand['level'] * 4
            if r == 'knowledge' and cand['type'] == 'pheromone_archive':
                c += 8
            costs[r] = c
        if any(self.res[r] < costs[r] for r in RES):
            return False
        for r in RES:
            self.add(r, -costs[r])
        cand['level'] += 1
        cand['progress'] = 0
        self.event('Upgrade start %s -> lvl %d' % (cand['type'], cand['level']))
        return True

    def enqueue_next(self, endgame):
        if self.queue or self.first_incomplete():
            return
        for t in STARTER_SEQUENCE:
            if not self.has_completed(t):
                self.queue.append(t)
                return
        if 'resin_masonry' in self.research and self.completed('resin_depot') < 1:
            self.queue.append('resin_depot')
            return
        if 'fungus_symbiosis' in self.research and self.completed('fungus_garden') < 2:
            self.queue.append('fungus_garden')
            return
        if 'venom_drills' in self.research and self.completed('venom_press') < 1:
            self.queue.append('venom_press')
            return
        if endgame:
            self.queue.append(endgame)
            self.event('Endgame planned ' + endgame)
            return
        p = self.priorities[0]
        if p == 'food':
            pb = 'food_store' if self.completed('food_store') < 2 else 'chitin_farm'
        elif p == 'ore':
            pb = 'mine' if self.completed('mine') < 2 else 'road'
        elif p == 'chitin':
            pb = 'chitin_farm' if self.completed('chitin_farm') < 3 else 'nursery'
        else:
            pb = 'watch_post' if self.completed('watch_post') < 4 else 'barracks'
        if pb:
            self.queue.append(pb)
            return

    def next_endgame(self):
        cit = self.rank() >= 3
        cc = self.completed
        if cc('great_mound') < 1 and cit and cc('queen_chamber') and cc('chitin_farm') and cc('market') and cc('pheromone_archive') and cc('armory') and cc('diplomacy_shrine'):
            return 'great_mound'
        if cc('queen_vault') < 1 and cit and cc('great_mound') and cc('nursery') and cc('pheromone_archive'):
            return 'queen_vault'
        if cc('trade_hub') < 1 and cit and cc('great_mound') and cc('queen_vault') and cc('market') and cc('diplomacy_shrine'):
            return 'trade_hub'
        return None

    def start_queued(self):
        if not self.queue:
            return False
        t = self.queue[0]
        ok = not (t == 'venom_press' and 'venom_drills' not in self.research) and all(
            self.res[r] >= self.eff_cost(t, r) for r in RES)
        if not ok:
            for r in RES:
                miss = self.eff_cost(t, r) - self.res[r]
                if miss > 0:
                    reason = 'construction ' + t
                    if not any(q['building'] == t and q['resource'] == r and q['reason'] == reason and q['fulfilled'] < q['needed'] for q in self.requests):
                        self.requests.append(dict(building=t, resource=r, needed=max(1, miss), fulfilled=0, reason=reason))
            return True
        self.requests = [q for q in self.requests if not (q['building'] == t and q['reason'] == 'construction ' + t)]
        for r in RES:
            self.add(r, -self.eff_cost(t, r))
        self.queue.pop(0)
        self.buildings.append(dict(type=t, level=1, progress=0, disabled=0))
        self.event('Started construction ' + t)
        return True

    def second(self):
        self.economy()
        self.logistics()
        self.jobloop()
        self.stageprog()
        self.balancer()
        self.native()
        self.builder()


def summary(c, t):
    castes = ' '.join('%s=%d' % (k[:3], c.castes[k]) for k in CASTE_ORDER if c.castes[k])
    res = ' '.join('%s=%d' % (r[:4], c.res[r]) for r in RES)
    done = sum(1 for b in c.buildings if b['progress'] >= 100)
    return 't=%4ds stage=%-11s rank=%-7s score=%3d pop=%2d upkeep=%3d built=%2d q=%s\n        %s\n        %s' % (
        t, STAGES[c.stage][0], RANKS[c.rank()][0], c.score(), c.pop(), c.upkeep(), done, c.queue, castes, res)


if __name__ == '__main__':
    culture = sys.argv[1] if len(sys.argv) > 1 else 'amber'
    rep = {'amber': 0}.get(culture, -10)
    seconds = int(sys.argv[2]) if len(sys.argv) > 2 else 600
    c = Colony(culture, rep)
    print(summary(c, 0))
    marks = {1, 2, 3, 5, 10, 20, 30, 60, 120, 180, 300, 600, 900, 1200, 1800, 3600}
    last_rank = c.rank()
    for t in range(1, seconds + 1):
        c.second()
        if c.rank() != last_rank:
            c.event('rank -> ' + RANKS[c.rank()][0])
            last_rank = c.rank()
        if t in marks:
            print(summary(c, t))
    print('--- event log (economy-tick second, message) ---')
    for e in c.log[:80]:
        print('   ', e)
    print('   ... total events', len(c.log))
