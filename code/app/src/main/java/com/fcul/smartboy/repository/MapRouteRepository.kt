package com.fcul.smartboy.repository

import com.fcul.smartboy.domain.route.MapRoute
import com.fcul.smartboy.repository.base.CRUD

class MapRouteRepository : CRUD<MapRoute, Long> {
    override suspend fun create(document: MapRoute): Long {
        // Implementation for creating a MapRoute entry
        TODO("Not yet implemented")
    }

    override suspend fun read(id: Long): MapRoute {
        // Implementation for reading a MapRoute entry
        TODO("Not yet implemented")
    }

    override suspend fun update(id: Long, data: Any): Boolean {
        // Implementation for updating a Route entry
        TODO("Not yet implemented")
    }

    override suspend fun delete(id: Long): Boolean {
        // Implementation for deleting a Route entry
        TODO("Not yet implemented")
    }
}