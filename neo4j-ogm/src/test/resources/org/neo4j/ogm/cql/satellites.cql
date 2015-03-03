create (_0:Satellite {`launch_date`:'04 Oct 1957', `manned`:'N', `ref`:'Sputnik 1', `satellite`:'Sputnik 1', `updated`:1415536632285})
create (_1:Space_Program {`program`:'Soviet', `ref`:'Soviet', `updated`:1415536632286})
create (_2:Orbit {`orbit`:'Elliptical', `ref`:'Elliptical', `updated`:1415536632286})
create (_3:Location {`location`:'LEO', `ref`:'LEO', `updated`:1415536632286})
create (_4:Satellite {`launch_date`:'19 Feb 1986', `manned`:'Y', `ref`:'Mir', `satellite`:'Mir', `updated`:1415536633465})
create (_5:Orbit {`orbit`:'Circular', `ref`:'Circular', `updated`:1415536633465})
create (_6:Satellite {`launch_date`:'20 Nov 1998', `manned`:'Y', `ref`:'ISS', `satellite`:'ISS', `updated`:1415536633495})
create (_7:Space_Program {`program`:'International', `ref`:'International', `updated`:1415536633495})
create (_8:Satellite {`launch_date`:'14 May 1973', `manned`:'Y', `ref`:'SkyLab', `satellite`:'SkyLab', `updated`:1415536633502})
create (_9:Space_Program {`program`:'NASA', `ref`:'NASA', `updated`:1415536633502})
create (_10:Satellite {`launch_date`:'10 Jul 1962', `manned`:'N', `ref`:'Telstar 1', `satellite`:'Telstar 1', `updated`:1415536633506})
create (_11:Location {`location`:'MEO', `ref`:'MEO', `updated`:1415536633507})
create (_12:Satellite {`launch_date`:'26 Nov 1990', `manned`:'N', `ref`:'GPS USA-66', `satellite`:'GPS USA-66', `updated`:1415536633511})
create (_13:Satellite {`launch_date`:'17 Oct 1963', `manned`:'N', `ref`:'Vela 1A', `satellite`:'Vela 1A', `updated`:1415536633516})
create (_14:Location {`location`:'HEO', `ref`:'HEO', `updated`:1415536633517})
create (_15:Satellite {`launch_date`:'11 Feb 2013', `manned`:'N', `ref`:'Landsat 8', `satellite`:'Landsat 8', `updated`:1415536633521})
create (_16:Satellite {`launch_date`:'08 Feb 1990', `manned`:'N', `ref`:'Hubble', `satellite`:'Hubble', `updated`:1415536633530})
create (_17:Satellite {`launch_date`:'14 May 2009', `manned`:'N', `ref`:'Herschel', `satellite`:'Herschel', `updated`:1415536633535})
create (_18:Space_Program {`program`:'ESA', `ref`:'ESA', `updated`:1415536633536})
create (_19:Orbit {`orbit`:'Lissajous', `ref`:'Lissajous', `updated`:1415536633536})
create (_20:Location {`location`:'L2', `ref`:'L2', `updated`:1415536633536})
create (_21:Satellite {`launch_date`:'14 May 2009', `manned`:'N', `ref`:'Planck', `satellite`:'Planck', `updated`:1415536633539})
create _0-[:ORBITAL_LOCATION]->_3
create _0-[:ORBITAL_TYPE]->_2
create _1-[:INACTIVE]->_4
create _1-[:INACTIVE]->_0
create _4-[:ORBITAL_LOCATION]->_3
create _4-[:ORBITAL_TYPE]->_5
create _6-[:ORBITAL_LOCATION]->_3
create _6-[:ORBITAL_TYPE]->_5
create _7-[:INACTIVE]->_10
create _7-[:ACTIVE]->_16
create _7-[:ACTIVE]->_12
create _7-[:ACTIVE]->_6
create _8-[:ORBITAL_LOCATION]->_3
create _8-[:ORBITAL_TYPE]->_5
create _9-[:INACTIVE]->_13
create _9-[:INACTIVE]->_8
create _9-[:ACTIVE]->_15
create _10-[:ORBITAL_LOCATION]->_11
create _10-[:ORBITAL_TYPE]->_2
create _12-[:ORBITAL_LOCATION]->_11
create _12-[:ORBITAL_TYPE]->_5
create _13-[:ORBITAL_LOCATION]->_14
create _13-[:ORBITAL_TYPE]->_5
create _15-[:ORBITAL_LOCATION]->_3
create _15-[:ORBITAL_TYPE]->_5
create _16-[:ORBITAL_LOCATION]->_3
create _16-[:ORBITAL_TYPE]->_5
create _17-[:ORBITAL_LOCATION]->_20
create _17-[:ORBITAL_TYPE]->_19
create _18-[:INACTIVE]->_17
create _18-[:ACTIVE]->_21
create _21-[:ORBITAL_LOCATION]->_20
create _21-[:ORBITAL_TYPE]->_19
