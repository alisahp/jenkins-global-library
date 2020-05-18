using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;

namespace DevOpsLeadsApi.Api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class LeadsController : ControllerBase
    {
        private readonly ILeadsService _leadsService;
        private readonly ILogger<LeadsController> _logger;
        private IEnumerable<Lead> Leads => _leadsService.GetLeads();

        public LeadsController(ILeadsService leadsService, ILogger<LeadsController> logger)
        {
            _leadsService = leadsService;
            _logger = logger;
        }

        [HttpGet]
        public ActionResult<IEnumerable<Lead>> Get()
        {
            return Ok(Leads);
        }
        
        [HttpGet("{id}")]
        public ActionResult<Lead> GetLead(int id)
        {
            var lead = Leads.SingleOrDefault(l => l.Id == id);
            if (lead == null)
            {
                return NotFound();
            }
            return Ok(lead);
        }
        
        [HttpPost]
        public ActionResult<Lead> CreateLead(Lead lead)
        {
            _leadsService.Add(lead);
            return CreatedAtAction(nameof(GetLead), new { id = lead.Id }, lead);
        }
        
        [HttpPut("{id}")]
        public IActionResult UpdateLead(long id, Lead lead)
        {
            if (id != lead.Id)
            {
                return BadRequest();
            }
            
            try
            {
                _leadsService.Update(lead);
            }
            catch
            {
                return UnprocessableEntity();
            }
            return NoContent();
        }
        
        [HttpDelete("{id}")]
        public IActionResult DeleteLead(int id)
        {
            try
            {
                _leadsService.Delete(id);
            }
            catch
            {
                return BadRequest();
            }
            return Ok();
        }

    }
}