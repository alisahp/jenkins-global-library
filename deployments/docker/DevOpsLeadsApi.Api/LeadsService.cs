using System;
using System.Collections.Generic;
using System.Linq;

namespace DevOpsLeadsApi.Api
{
    public interface ILeadsService
    {
        IEnumerable<Lead> GetLeads();
        Lead Add(Lead lead);
        void Update(Lead lead);
        void Delete(int id);
    }
    
    public class LeadsService : ILeadsService
    {
        private readonly IList<Lead> _leads;
        public static readonly string[] Services = new[]
        {
            "Plumbing", "Painting", "Remodeling", "Roofing", "Flooring"
        };
        
        public LeadsService()
        {
            _leads = BuildLeads();
        }
        
        public IEnumerable<Lead> GetLeads()
        {
            return _leads;
        }
        
        public Lead Add(Lead lead)
        {
            lead.Id = _leads.Max(l => l.Id) + 1;
            _leads.Add(lead);
            return lead;
        }
        
        public void Update(Lead lead)
        {
            if (_leads.Any(l => l.Id == lead.Id))
            {
                _leads.Remove(lead);
                _leads.Add(lead);
            }
        }
        
        public void Delete(int id)
        {
            var lead = _leads.Single(l => l.Id == id);
            _leads.Remove(lead);
        }
        
        private static IList<Lead> BuildLeads()
        {
            var rng = new Random();
            return Enumerable.Range(1, 5).Select(index => 
                new Lead(index, Faker.Name.First(), Faker.Name.Last())
                {
                    Address = Faker.Address.StreetAddress(),
                    Service = Services[rng.Next(Services.Length)]
                }).ToList();
        }
    }
}