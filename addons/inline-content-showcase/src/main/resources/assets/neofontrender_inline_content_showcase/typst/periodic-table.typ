#set text(size: 9pt);
#let symbols = "H He Li Be B C N O F Ne Na Mg Al Si P S Cl Ar K Ca Sc Ti V Cr Mn Fe Co Ni Cu Zn Ga Ge As Se Br Kr Rb Sr Y Zr Nb Mo Tc Ru Rh Pd Ag Cd In Sn Sb Te I Xe Cs Ba La Ce Pr Nd Pm Sm Eu Gd Tb Dy Ho Er Tm Yb Lu Hf Ta W Re Os Ir Pt Au Hg Tl Pb Bi Po At Rn Fr Ra Ac Th Pa U Np Pu Am Cm Bk Cf Es Fm Md No Lr Rf Db Sg Bh Hs Mt Ds Rg Cn Nh Fl Mc Lv Ts Og".split(" ");
#let colors = (rgb("ef9b8b"),rgb("efcc85"),rgb("8ccac8"),rgb("afbee8"),rgb("c6b2de"),rgb("9fd49b"),rgb("e2d493"),rgb("84cce9"),rgb("e8acd0"),rgb("d0b39a"));
#let group(z) = {
  if z >= 57 and z <= 71 { 8 }
  else if z >= 89 and z <= 103 { 9 }
  else if (3,11,19,37,55,87).contains(z) { 0 }
  else if (4,12,20,38,56,88).contains(z) { 1 }
  else if (2,10,18,36,54,86,118).contains(z) { 7 }
  else if (9,17,35,53,85,117).contains(z) { 6 }
  else if (1,6,7,8,15,16,34).contains(z) { 5 }
  else if (5,14,32,33,51,52).contains(z) { 4 }
  else if (13,31,49,50,81,82,83,84,113,114,115,116).contains(z) { 3 }
  else { 2 }
};
#let cell(z) = box(width: 23pt, height: 25pt, fill: colors.at(group(z)), radius: 2pt, inset: 2pt)[
  #set text(fill: rgb("172536"));
  #text(size: 5pt, str(z)) #linebreak()
  #align(center, text(weight: "bold", size: 10pt, symbols.at(z - 1)))
];
#let blank = box(width: 23pt, height: 25pt);
#let row(values) = grid(columns: (23pt,) * 18, gutter: 2pt, ..values.map(z => if z == 0 { blank } else { cell(z) }));
#let gap(label) = box(width: 23pt, height: 25pt, inset: 1pt, stroke: rgb("8293a9"), radius: 2pt, align(center + horizon, text(size: 5pt, label)));
#stack(dir: ttb, spacing: 2pt,
  align(center, text(size: 15pt, weight: "bold", "PERIODIC TABLE OF THE ELEMENTS")),
  grid(columns: (23pt,) * 18, gutter: 2pt, ..range(1,19).map(g => align(center,text(size: 6pt,str(g))))),
  row((1,) + (0,) * 16 + (2,)),
  row((3,4) + (0,) * 10 + range(5,11)),
  row((11,12) + (0,) * 10 + range(13,19)),
  row(range(19,37)),
  row(range(37,55)),
  grid(columns: (23pt,) * 18, gutter: 2pt, cell(55),cell(56),gap("57–71"),..range(72,87).map(cell)),
  grid(columns: (23pt,) * 18, gutter: 2pt, cell(87),cell(88),gap("89–103"),..range(104,119).map(cell)),
  v(4pt),
  row((0,0,0) + range(57,72)),
  row((0,0,0) + range(89,104)),
  v(4pt),
  grid(columns: 5, column-gutter: 6pt, row-gutter: 3pt, ..("Alkali metals","Alkaline earths","Transition metals","Post-transition","Metalloids","Nonmetals","Halogens","Noble gases","Lanthanides","Actinides").enumerate().map(((i,label)) => [#box(width: 6pt,height: 6pt,fill: colors.at(i)) #text(size: 6pt,label)])),
)
