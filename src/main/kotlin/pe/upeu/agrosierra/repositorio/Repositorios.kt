package pe.upeu.agrosierra.repositorio

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import pe.upeu.agrosierra.dominio.*

interface RolRepository : JpaRepository<Rol, Long> {
    fun findByNombre(nombre: String): Rol?
}

interface UsuarioRepository : JpaRepository<Usuario, Long> {
    fun findByUsuario(usuario: String): Usuario?
}

interface LoteRepository : JpaRepository<Lote, Long> {
    @Query("""SELECT l FROM Lote l WHERE (:valle IS NULL OR LOWER(l.valle) = LOWER(:valle)) AND (:cerrada IS NULL OR l.campaniaCerrada = :cerrada)""")
    fun filtrar(@Param("valle") valle: String?, @Param("cerrada") cerrada: Boolean?, pageable: Pageable): Page<Lote>
}

interface LaborRepository : JpaRepository<Labor, Long> {
    fun findByLoteId(id: Long, pageable: Pageable): Page<Labor>
    fun existsByLoteIdAndTipo(id: Long, tipo: TipoLabor): Boolean
}
