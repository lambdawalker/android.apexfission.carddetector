/** Catalogs link to concrete scopes in their own language, never legacy development URLs. */
export function catalogLinks(scopes,language) {
 return scopes.filter(scope=>scope.language===language).map(scope=>({
  title:`${scope.module} · ${scope.version==='development'?(language==='es'?'Desarrollo':'Development'):scope.version}`,
  href:scope.root,
 }));
}
